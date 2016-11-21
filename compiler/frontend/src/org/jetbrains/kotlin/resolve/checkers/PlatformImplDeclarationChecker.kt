/*
 * Copyright 2010-2016 JetBrains s.r.o.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.jetbrains.kotlin.resolve.checkers

import org.jetbrains.kotlin.config.LanguageFeature
import org.jetbrains.kotlin.config.LanguageVersionSettings
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.checkers.PlatformImplDeclarationChecker.Compatibility.Compatible
import org.jetbrains.kotlin.resolve.checkers.PlatformImplDeclarationChecker.Compatibility.Incompatible
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameUnsafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.getDescriptorsFiltered
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeConstructorSubstitution
import org.jetbrains.kotlin.types.TypeSubstitutor
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.utils.keysToMap

class PlatformImplDeclarationChecker : DeclarationChecker {
    override fun check(
            declaration: KtDeclaration,
            descriptor: DeclarationDescriptor,
            diagnosticHolder: DiagnosticSink,
            bindingContext: BindingContext,
            languageVersionSettings: LanguageVersionSettings
    ) {
        if (!languageVersionSettings.supportsFeature(LanguageFeature.MultiPlatformProjects)) return

        if (descriptor !is MemberDescriptor) return

        if (descriptor.isPlatform && declaration.hasModifier(KtTokens.PLATFORM_KEYWORD)) {
            checkPlatformDeclarationHasDefinition(declaration, descriptor, diagnosticHolder)
        }
        else if (descriptor.isImpl && declaration.hasModifier(KtTokens.IMPL_KEYWORD)) {
            checkImplementationHasPlatformDeclaration(declaration, descriptor, diagnosticHolder)
        }
    }

    private fun checkPlatformDeclarationHasDefinition(
            reportOn: KtDeclaration, descriptor: MemberDescriptor, diagnosticHolder: DiagnosticSink
    ) {
        val compatibility = when (descriptor) {
            is CallableMemberDescriptor -> {
                descriptor.findNamesakesFromTheSameModule().filter { impl ->
                    descriptor != impl &&
                    // TODO: support non-source definitions (e.g. from Java)
                    DescriptorToSourceUtils.getSourceFromDescriptor(impl) is KtElement
                }.groupBy { impl ->
                    areCompatibleCallables(descriptor, impl)
                }
            }
            is ClassDescriptor -> {
                descriptor.findClassifiersFromTheSameModule().filter { impl ->
                    descriptor != impl &&
                    DescriptorToSourceUtils.getSourceFromDescriptor(impl) is KtElement
                }.groupBy { impl ->
                    areCompatibleClassifiers(descriptor, impl)
                }
            }
            else -> null
        }

        if (compatibility != null && !compatibility.containsKey(Compatible)) {
            assert(compatibility.keys.all { it is Incompatible })
            @Suppress("UNCHECKED_CAST")
            val incompatibility = compatibility as Map<Incompatible, Collection<MemberDescriptor>>
            diagnosticHolder.report(Errors.PLATFORM_DECLARATION_WITHOUT_DEFINITION.on(reportOn, descriptor, incompatibility))
        }
    }

    private fun checkImplementationHasPlatformDeclaration(
            reportOn: KtDeclaration, descriptor: MemberDescriptor, diagnosticHolder: DiagnosticSink
    ) {
        val hasDeclaration = when (descriptor) {
            is CallableMemberDescriptor -> descriptor.findNamesakesFromTheSameModule().any { declaration ->
                descriptor != declaration &&
                declaration.isPlatform &&
                areCompatibleCallables(declaration, descriptor) == Compatible
            }
            is ClassifierDescriptor -> descriptor.findClassifiersFromTheSameModule().any { declaration ->
                descriptor != declaration &&
                declaration is ClassDescriptor && declaration.isPlatform &&
                areCompatibleClassifiers(declaration, descriptor) == Compatible
            }
            else -> false
        }

        if (!hasDeclaration) {
            // TODO: do not report this error for members which are "almost compatible" with some platform declarations
            diagnosticHolder.report(Errors.PLATFORM_DEFINITION_WITHOUT_DECLARATION.on(reportOn.modifierList!!.getModifier(KtTokens.IMPL_KEYWORD)!!))
        }
    }

    private fun CallableMemberDescriptor.findNamesakesFromTheSameModule(): Collection<CallableMemberDescriptor> {
        val packageFqName = (containingDeclaration as? PackageFragmentDescriptor)?.fqName ?: return emptyList()
        val myModule = this.module
        val scope = myModule.getPackage(packageFqName).memberScope

        return when (this) {
            is FunctionDescriptor -> scope.getContributedFunctions(name, NoLookupLocation.FOR_ALREADY_TRACKED)
            is PropertyDescriptor -> scope.getContributedVariables(name, NoLookupLocation.FOR_ALREADY_TRACKED)
            else -> throw AssertionError("Unsupported declaration: $this")
        }.filter {
            it.module == myModule // TODO: only obtain descriptors from our module to start with
        }
    }

    private fun ClassifierDescriptor.findClassifiersFromTheSameModule(): Collection<ClassifierDescriptor> {
        // TODO: support nested classes
        return module.getPackage(fqNameSafe.parent()).memberScope
                .getDescriptorsFiltered(DescriptorKindFilter.CLASSIFIERS) { it == name }
                .filterIsInstance<ClassifierDescriptor>()
    }

    sealed class Compatibility {
        // Note that the reason is used in the diagnostic output, see PlatformIncompatibilityDiagnosticRenderer
        sealed class Incompatible(val reason: String?) : Compatibility() {
            // Callables

            object ParameterShape : Incompatible("parameter shapes are different (extension vs non-extension)")

            object ParameterCount : Incompatible("number of value parameters is different")
            object TypeParameterCount : Incompatible("number of type parameters is different")

            object ParameterTypes : Incompatible("parameter types are different")
            object ReturnType : Incompatible("return type is different")

            object ParameterNames : Incompatible("parameter names are different")
            object TypeParameterNames : Incompatible("names of type parameters are different")

            object ValueParameterHasDefault : Incompatible("some parameters have default values")
            object ValueParameterModifiers : Incompatible("parameter modifiers are different (vararg, coroutine, crossinline, noinline)")

            // Functions

            object FunctionModifiers : Incompatible("modifiers are different (external, infix, inline, operator, suspend, tailrec)")

            // Properties

            object PropertyKind : Incompatible("property kinds are different (val vs var)")
            object PropertyModifiers : Incompatible("modifiers are different (const, lateinit)")

            // Classifiers

            object ClassKind : Incompatible("class kinds are different (class, interface, object, enum, annotation)")

            object ClassModifiers : Incompatible("modifiers are different (data)")

            object Supertypes : Incompatible("some supertypes are missing in the implementation")

            // Common

            object Modality : Incompatible("modality is different")
            object Visibility : Incompatible("visibility is different")

            object TypeParameterUpperBounds : Incompatible("upper bounds of type parameters are different")
            object TypeParameterVariance : Incompatible("declaration-site variances of type parameters are different")
            object TypeParameterReified : Incompatible("some type parameter is reified in one declaration and non-reified in the other")

            object Unknown : Incompatible(null)
        }

        object Compatible : Compatibility()
    }

    // a is the declaration in common code, b is the definition in the platform-specific code
    private fun areCompatibleCallables(a: CallableMemberDescriptor, b: CallableMemberDescriptor): Compatibility {
        assert(a.name == b.name) { "This function should be invoked only for declarations with the same name: $a, $b" }
        assert(a.containingDeclaration is ClassDescriptor == b.containingDeclaration is ClassDescriptor) {
            "This function should be invoked only for declarations in the same kind of container (both members or both top level): $a, $b"
        }

        val aExtensionReceiver = a.extensionReceiverParameter
        val bExtensionReceiver = b.extensionReceiverParameter
        if ((aExtensionReceiver != null) != (bExtensionReceiver != null)) return Incompatible.ParameterShape

        val aParams = a.valueParameters
        val bParams = b.valueParameters
        if (aParams.size != bParams.size) return Incompatible.ParameterCount

        val aTypeParams = a.typeParameters
        val bTypeParams = b.typeParameters
        if (aTypeParams.size != bTypeParams.size) return Incompatible.TypeParameterCount

        val substitutor = Substitutor(aTypeParams, bTypeParams)

        if (aParams.map { substitutor(it.type) } != bParams.map { it.type } ||
            aExtensionReceiver?.type?.let(substitutor) != bExtensionReceiver?.type) return Incompatible.ParameterTypes
        if (substitutor(a.returnType) != b.returnType) return Incompatible.ReturnType

        if (!equalsBy(aParams, bParams, ValueParameterDescriptor::getName)) return Incompatible.ParameterNames
        if (!equalsBy(aTypeParams, bTypeParams, TypeParameterDescriptor::getName)) return Incompatible.TypeParameterNames

        if (a.modality != b.modality) return Incompatible.Modality
        if (a.visibility != b.visibility) return Incompatible.Visibility

        areCompatibleTypeParameters(aTypeParams, bTypeParams, substitutor).let { if (it != Compatible) return it }

        if (!equalsBy(aParams, bParams, ValueParameterDescriptor::declaresDefaultValue)) return Incompatible.ValueParameterHasDefault
        if (!equalsBy(aParams, bParams, { p -> listOf(p.varargElementType != null, p.isCoroutine, p.isCrossinline, p.isNoinline) })) return Incompatible.ValueParameterModifiers

        when {
            a is FunctionDescriptor && b is FunctionDescriptor -> areCompatibleFunctions(a, b).let { if (it != Compatible) return it }
            a is PropertyDescriptor && b is PropertyDescriptor -> areCompatibleProperties(a, b).let { if (it != Compatible) return it }
            else -> throw AssertionError("Unsupported declarations: $a, $b")
        }

        // TODO: check 'impl' modifier

        return Compatible
    }

    private fun areCompatibleTypeParameters(a: List<TypeParameterDescriptor>, b: List<TypeParameterDescriptor>, substitutor: Substitutor): Compatibility {
        if (a.map { substitutor(it.defaultType) } != b.map { it.defaultType }) return Incompatible.TypeParameterUpperBounds
        if (!equalsBy(a, b, TypeParameterDescriptor::getVariance)) return Incompatible.TypeParameterVariance
        if (!equalsBy(a, b, TypeParameterDescriptor::isReified)) return Incompatible.TypeParameterReified

        return Compatible
    }

    private fun areCompatibleFunctions(a: FunctionDescriptor, b: FunctionDescriptor): Compatibility {
        if (!equalBy(a, b) { f ->
            listOf(f.isExternal, f.isInfix, f.isInline, f.isOperator, f.isSuspend, f.isTailrec)
        }) return Incompatible.FunctionModifiers

        return Compatible
    }

    private fun areCompatibleProperties(a: PropertyDescriptor, b: PropertyDescriptor): Compatibility {
        if (!equalBy(a, b) { p -> p.isVar }) return Incompatible.PropertyKind
        if (!equalBy(a, b) { p -> listOf(p.isConst, p.isLateInit) }) return Incompatible.PropertyModifiers

        return Compatible
    }

    private fun areCompatibleClassifiers(a: ClassDescriptor, other: ClassifierDescriptor): Compatibility {
        assert(a.fqNameUnsafe == other.fqNameUnsafe) { "This function should be invoked only for declarations with the same name: $a, $other" }

        val b = when (other) {
            is ClassDescriptor -> other
            is TypeAliasDescriptor -> other.classDescriptor ?: return Incompatible.Unknown
            else -> throw AssertionError("Incorrect impl classifier for $a: $other")
        }

        if (a.kind != b.kind) return Incompatible.ClassKind

        val aTypeParams = a.declaredTypeParameters
        val bTypeParams = b.declaredTypeParameters
        if (aTypeParams.size != bTypeParams.size) return Incompatible.TypeParameterCount

        val substitutor = Substitutor(aTypeParams, bTypeParams)

        if (a.modality != b.modality) return Incompatible.Modality
        if (a.visibility != b.visibility) return Incompatible.Visibility

        areCompatibleTypeParameters(aTypeParams, bTypeParams, substitutor).let { if (it != Compatible) return it }

        if (!equalBy(a, b) { it.isData }) return Incompatible.ClassModifiers

        if (!b.typeConstructor.supertypes.containsAll(a.typeConstructor.supertypes.map(substitutor))) return Incompatible.Supertypes

        // TODO: check scopes
        // TODO: check 'impl' modifier

        return Compatible
    }

    private inline fun <T, K> equalBy(first: T, second: T, selector: (T) -> K): Boolean =
            selector(first) == selector(second)

    private inline fun <T, K> equalsBy(first: List<T>, second: List<T>, selector: (T) -> K): Boolean {
        for (i in first.indices) {
            if (selector(first[i]) != selector(second[i])) return false
        }

        return true
    }

    // This substitutor takes the type from A's signature and returns the type that should be in that place in B's signature
    private class Substitutor(
            aTypeParams: List<TypeParameterDescriptor>,
            bTypeParams: List<TypeParameterDescriptor>
    ) : (KotlinType?) -> KotlinType? {
        val typeSubstitutor = TypeSubstitutor.create(TypeConstructorSubstitution.createByParametersMap(
                aTypeParams.keysToMap { bTypeParams[it.index].defaultType.asTypeProjection() }
        ))

        override fun invoke(type: KotlinType?): KotlinType? =
                type?.asTypeProjection()?.let(typeSubstitutor::substitute)?.type
    }
}
