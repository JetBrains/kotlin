/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.idea.fir.fe10

import org.jetbrains.kotlin.builtins.DefaultBuiltIns
import org.jetbrains.kotlin.builtins.KotlinBuiltIns
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.idea.frontend.api.KtStarProjectionTypeArgument
import org.jetbrains.kotlin.idea.frontend.api.KtTypeArgument
import org.jetbrains.kotlin.idea.frontend.api.KtTypeArgumentWithVariance
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtAnonymousObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtNamedClassOrObjectSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.KtTypeAliasSymbol
import org.jetbrains.kotlin.idea.frontend.api.symbols.markers.KtTypeAndAnnotations
import org.jetbrains.kotlin.idea.frontend.api.types.*
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.scopes.DescriptorKindFilter
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.types.*
import org.jetbrains.kotlin.types.checker.KotlinTypeRefiner
import org.jetbrains.kotlin.types.refinement.TypeRefinement
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

abstract class KtSymbolBasedAbstractTypeConstructor<T> internal constructor(
    val ktSBDescriptor: T
) : ClassifierBasedTypeConstructor() where T : KtSymbolBasedDeclarationDescriptor, T : ClassifierDescriptor {
    override fun getDeclarationDescriptor(): ClassifierDescriptor = ktSBDescriptor

    // TODO: captured types
    override fun isDenotable(): Boolean = true

    // for Intention|inspection it shouldn't be important what to use.
    override fun getBuiltIns(): KotlinBuiltIns = DefaultBuiltIns.Instance

    // I don't think that we need to implement this method
    override fun isFinal(): Boolean = ktSBDescriptor.context.implementationPostponed("ktSBDescriptor = $ktSBDescriptor")

    @TypeRefinement
    override fun refine(kotlinTypeRefiner: KotlinTypeRefiner): TypeConstructor =
        ktSBDescriptor.context.noImplementation("ktSBDescriptor = $ktSBDescriptor")
}

class KtSymbolBasedClassTypeConstructor(ktSBDescriptor: KtSymbolBasedClassDescriptor) :
    KtSymbolBasedAbstractTypeConstructor<KtSymbolBasedClassDescriptor>(ktSBDescriptor) {
    override fun getParameters(): List<TypeParameterDescriptor> =
        ktSBDescriptor.ktSymbol.typeParameters.map { KtSymbolBasedTypeParameterDescriptor(it, ktSBDescriptor.context) }

    override fun getSupertypes(): Collection<KotlinType> =
        ktSBDescriptor.ktSymbol.superTypes.map { it.toKotlinType(ktSBDescriptor.context) }

    override fun isSameClassifier(classifier: ClassifierDescriptor): Boolean {
        return classifier is ClassDescriptor && areFqNamesEqual(declarationDescriptor, classifier)
    }

    override fun toString() = DescriptorUtils.getFqName(ktSBDescriptor).asString()
}

class KtSymbolBasedTypeParameterTypeConstructor(ktSBDescriptor: KtSymbolBasedTypeParameterDescriptor) :
    KtSymbolBasedAbstractTypeConstructor<KtSymbolBasedTypeParameterDescriptor>(ktSBDescriptor) {
    override fun getParameters(): List<TypeParameterDescriptor> = emptyList()

    override fun getSupertypes(): Collection<KotlinType> =
        ktSBDescriptor.ktSymbol.upperBounds.map { it.toKotlinType(ktSBDescriptor.context) }

    // TODO overrides: see AbstractTypeParameterDescriptor.TypeParameterTypeConstructor.isSameClassifier
    override fun isSameClassifier(classifier: ClassifierDescriptor): Boolean = ktSBDescriptor == classifier

    override fun toString(): String = ktSBDescriptor.name.asString()
}

// This class is not suppose to be used as "is instance of" because scopes could be wrapped into other scopes
// so generally it isn't a good idea
internal class MemberScopeForKtSymbolBasedDescriptors(lazyDebugInfo: () -> String) : MemberScope {
    private val additionalInfo by lazy(lazyDebugInfo)

    private fun noImplementation(): Nothing =
        error("Scope for descriptors based on KtSymbols should not be used, additional info: $additionalInfo")

    override fun getContributedVariables(name: Name, location: LookupLocation): Collection<PropertyDescriptor> = noImplementation()
    override fun getContributedFunctions(name: Name, location: LookupLocation): Collection<SimpleFunctionDescriptor> = noImplementation()
    override fun getFunctionNames(): Set<Name> = noImplementation()
    override fun getVariableNames(): Set<Name> = noImplementation()
    override fun getClassifierNames(): Set<Name> = noImplementation()
    override fun printScopeStructure(p: Printer): Unit = noImplementation()
    override fun getContributedClassifier(name: Name, location: LookupLocation): ClassifierDescriptor = noImplementation()

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> = noImplementation()
}

fun KtTypeAndAnnotations.getDescriptorsAnnotations(context: FE10BindingContext): Annotations =
    Annotations.create(annotations.map { KtSymbolBasedAnnotationDescriptor(it, context) })

fun KtTypeAndAnnotations.toKotlinType(context: FE10BindingContext): UnwrappedType =
    type.toKotlinType(context, getDescriptorsAnnotations(context))

fun KtTypeArgument.toTypeProjection(context: FE10BindingContext): TypeProjection =
    when (this) {
        is KtStarProjectionTypeArgument -> StarProjectionForAbsentTypeParameter(context.builtIns)
        is KtTypeArgumentWithVariance -> TypeProjectionImpl(variance, type.toKotlinType(context))
    }

fun KtType.toKotlinType(context: FE10BindingContext, annotations: Annotations = Annotations.EMPTY): UnwrappedType {
    val typeConstructor: TypeConstructor = when (this) {
        is KtTypeParameterType -> KtSymbolBasedTypeParameterDescriptor(this.symbol, context).typeConstructor
        is KtNonErrorClassType -> when (val classLikeSymbol = classSymbol) {
            is KtTypeAliasSymbol -> context.typeAliasImplementationPlanned()
            is KtNamedClassOrObjectSymbol -> KtSymbolBasedClassDescriptor(classLikeSymbol, context).typeConstructor
            is KtAnonymousObjectSymbol -> context.implementationPostponed()
        }
        is KtClassErrorType -> ErrorUtils.createErrorTypeConstructorWithCustomDebugName(error)
        is KtFlexibleType -> {
            return KotlinTypeFactory.flexibleType(
                lowerBound.toKotlinType(context, annotations) as SimpleType,
                upperBound.toKotlinType(context, annotations) as SimpleType
            )
        }

        is KtIntersectionType -> {
            // most likely it isn't correct and intersectTypes(List<UnwrappedType>) should be used,
            // but I don't think that we will have the real problem with that implementation
            return IntersectionTypeConstructor(conjuncts.map { it.toKotlinType(context) }).createType()
        }
        else -> error("Unexpected subclass: ${this.javaClass}")
    }

    val ktTypeArguments = this.safeAs<KtNonErrorClassType>()?.typeArguments ?: emptyList()

    val markedAsNullable = this.safeAs<KtTypeWithNullability>()?.nullability == KtTypeNullability.NULLABLE

    return KotlinTypeFactory.simpleTypeWithNonTrivialMemberScope(
        annotations, typeConstructor, ktTypeArguments.map { it.toTypeProjection(context) }, markedAsNullable,
        MemberScopeForKtSymbolBasedDescriptors { this.asStringForDebugging() }
    )
}