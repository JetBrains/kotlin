/*
 * Copyright 2010-2018 JetBrains s.r.o. Use of this source code is governed by the Apache 2.0 license
 * that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.backend.common

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.backend.common.bridges.findInterfaceImplementation
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorToSourceUtils
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.checkers.ExpectedActualDeclarationChecker
import org.jetbrains.kotlin.resolve.multiplatform.ExpectedActualResolver
import org.jetbrains.kotlin.types.KotlinType

object CodegenUtil {
    @JvmStatic
    fun getDelegatePropertyIfAny(
            expression: KtExpression, classDescriptor: ClassDescriptor, bindingContext: BindingContext
    ): PropertyDescriptor? {
        val call = (expression as? KtSimpleNameExpression)?.getResolvedCall(bindingContext) ?: return null
        val callResultingDescriptor = call.resultingDescriptor as? ValueParameterDescriptor ?: return null
        // constructor parameter
        if (callResultingDescriptor.containingDeclaration is ConstructorDescriptor) {
            // constructor of my class
            if (callResultingDescriptor.containingDeclaration.containingDeclaration === classDescriptor) {
                return bindingContext.get(BindingContext.VALUE_PARAMETER_AS_PROPERTY, callResultingDescriptor)
            }
        }
        return null
    }

    @JvmStatic
    fun isFinalPropertyWithBackingField(propertyDescriptor: PropertyDescriptor?, bindingContext: BindingContext): Boolean {
        return propertyDescriptor != null &&
               !propertyDescriptor.isVar &&
               (bindingContext.get(BindingContext.BACKING_FIELD_REQUIRED, propertyDescriptor) ?: false)
    }

    @JvmStatic
    @JvmOverloads
    fun getNonPrivateTraitMethods(descriptor: ClassDescriptor, copy: Boolean = true): Map<FunctionDescriptor, FunctionDescriptor> {
        val result = linkedMapOf<FunctionDescriptor, FunctionDescriptor>()
        for (declaration in DescriptorUtils.getAllDescriptors(descriptor.defaultType.memberScope)) {
            if (declaration !is CallableMemberDescriptor) continue

            val traitMember = findInterfaceImplementation(declaration)
            if (traitMember == null ||
                    Visibilities.isPrivate(traitMember.visibility) ||
                    traitMember.visibility == Visibilities.INVISIBLE_FAKE) continue

            assert(traitMember.modality !== Modality.ABSTRACT) { "Cannot delegate to abstract trait method: $declaration" }

            // inheritedMember can be abstract here. In order for FunctionCodegen to generate the method body, we're creating a copy here
            // with traitMember's modality
            result.putAll(
                if (copy)
                    copyFunctions(
                        declaration, traitMember, declaration.containingDeclaration, traitMember.modality,
                        Visibilities.PUBLIC, CallableMemberDescriptor.Kind.DECLARATION, true
                    )
                else mapMembers(declaration, traitMember)
            )
        }
        return result
    }

    fun copyFunctions(
        inheritedMember: CallableMemberDescriptor,
        traitMember: CallableMemberDescriptor,
        newOwner: DeclarationDescriptor,
        modality: Modality,
        visibility: Visibility,
        kind: CallableMemberDescriptor.Kind,
        copyOverrides: Boolean
    ): Map<FunctionDescriptor, FunctionDescriptor> =
        mapMembers(inheritedMember.copy(newOwner, modality, visibility, kind, copyOverrides), traitMember)

    private fun mapMembers(
        inherited: CallableMemberDescriptor,
        traitMember: CallableMemberDescriptor
    ): LinkedHashMap<FunctionDescriptor, FunctionDescriptor> {
        val result = linkedMapOf<FunctionDescriptor, FunctionDescriptor>()
        if (traitMember is SimpleFunctionDescriptor) {
            result[traitMember] = inherited as FunctionDescriptor
        } else if (traitMember is PropertyDescriptor) {
            for (traitAccessor in traitMember.accessors) {
                for (inheritedAccessor in (inherited as PropertyDescriptor).accessors) {
                    if (inheritedAccessor::class.java == traitAccessor::class.java) { // same accessor kind
                        result.put(traitAccessor, inheritedAccessor)
                    }
                }
            }
        }
        return result
    }

    @JvmStatic
    fun getSuperClassBySuperTypeListEntry(specifier: KtSuperTypeListEntry, bindingContext: BindingContext): ClassDescriptor? {
        val superType = bindingContext.get(BindingContext.TYPE, specifier.typeReference!!)

        return superType?.constructor?.declarationDescriptor as? ClassDescriptor
    }

    @JvmStatic
    fun getLineNumberForElement(statement: PsiElement, markEndOffset: Boolean): Int? {
        val file = statement.containingFile
        if (file is KtFile && file.doNotAnalyze != null) {
            return null
        }

        if (statement is KtConstructorDelegationReferenceExpression && statement.textLength == 0) {
            // PsiElement for constructor delegation reference is always generated, so we shouldn't mark it's line number if it's empty
            return null
        }

        val document = file.viewProvider.document
        return document?.getLineNumber(if (markEndOffset) statement.textRange.endOffset else statement.textOffset)?.plus(1)
    }

    // Returns the descriptor for a function (whose parameters match the given predicate) which should be generated in the class.
    // Note that we always generate equals/hashCode/toString in data classes, unless that would lead to a JVM signature clash with
    // another method, which can only happen if the method is declared in the data class (manually or via delegation).
    // Also there are no hard asserts or assumptions because such methods are generated for erroneous code as well (in light classes mode).
    fun getMemberToGenerate(
            classDescriptor: ClassDescriptor,
            name: String,
            isReturnTypeOk: (KotlinType) -> Boolean,
            areParametersOk: (List<ValueParameterDescriptor>) -> Boolean
    ): FunctionDescriptor? =
            classDescriptor.unsubstitutedMemberScope.getContributedFunctions(Name.identifier(name), NoLookupLocation.FROM_BACKEND)
                    .singleOrNull { function ->
                        function.kind.let { kind -> kind == CallableMemberDescriptor.Kind.SYNTHESIZED || kind == CallableMemberDescriptor.Kind.FAKE_OVERRIDE } &&
                        function.modality != Modality.FINAL &&
                        areParametersOk(function.valueParameters) &&
                        function.returnType != null &&
                        isReturnTypeOk(function.returnType!!)
                    }


    @JvmStatic
    fun isExhaustive(bindingContext: BindingContext, whenExpression: KtWhenExpression, isStatement: Boolean): Boolean {
        val slice = if (isStatement && !whenExpression.isUsedAsExpression(bindingContext)) {
            BindingContext.IMPLICIT_EXHAUSTIVE_WHEN
        }
        else {
            BindingContext.EXHAUSTIVE_WHEN
        }
        return bindingContext[slice, whenExpression] == true
    }

    @JvmStatic
    fun constructFakeFunctionCall(project: Project, arity: Int): KtCallExpression {
        val fakeFunctionCall =
                (1..arity).joinToString(prefix = "callableReferenceFakeCall(", separator = ", ", postfix = ")") { "p$it" }
        return KtPsiFactory(project, markGenerated = false).createExpression(fakeFunctionCall) as KtCallExpression
    }

    /**
     * Returns declarations in the given [file] which should be generated by the back-end. This includes all declarations
     * minus all expected declarations (except annotation classes annotated with @OptionalExpectation).
     */
    @JvmStatic
    fun getDeclarationsToGenerate(file: KtFile, bindingContext: BindingContext): List<KtDeclaration> =
        file.declarations.filter(fun(declaration: KtDeclaration): Boolean {
            if (!declaration.hasExpectModifier()) return true

            if (declaration is KtClass) {
                val descriptor = bindingContext.get(BindingContext.CLASS, declaration)
                if (descriptor != null && ExpectedActualDeclarationChecker.shouldGenerateExpectClass(descriptor)) {
                    return true
                }
            }

            return false
        })

    @JvmStatic
    fun findExpectedFunctionForActual(descriptor: FunctionDescriptor): FunctionDescriptor? {
        val compatibleExpectedFunctions = with(ExpectedActualResolver) {
            descriptor.findCompatibleExpectedForActual(DescriptorUtils.getContainingModule(descriptor))
        }
        return compatibleExpectedFunctions.firstOrNull() as FunctionDescriptor?
    }

    @JvmStatic
    fun getFunctionParametersForDefaultValueGeneration(
        descriptor: FunctionDescriptor,
        trace: DiagnosticSink?
    ): List<ValueParameterDescriptor> {
        if (descriptor.isActual) {
            val expected = CodegenUtil.findExpectedFunctionForActual(descriptor)
            if (expected != null && expected.valueParameters.any(ValueParameterDescriptor::declaresDefaultValue)) {
                val element = DescriptorToSourceUtils.descriptorToDeclaration(expected)
                if (element == null) {
                    if (trace != null) {
                        val actualDeclaration = DescriptorToSourceUtils.descriptorToDeclaration(descriptor)
                                ?: error("Not a source declaration: $descriptor")
                        trace.report(Errors.EXPECTED_FUNCTION_SOURCE_WITH_DEFAULT_ARGUMENTS_NOT_FOUND.on(actualDeclaration))
                    }
                    return descriptor.valueParameters
                }

                return expected.valueParameters
            }
        }

        return descriptor.valueParameters
    }
}

fun DeclarationDescriptor.isTopLevelInPackage(name: String, packageName: String): Boolean {
    if (name != this.name.asString()) return false

    val containingDeclaration = containingDeclaration as? PackageFragmentDescriptor ?: return false
    val packageFqName = containingDeclaration.fqName.asString()
    return packageName == packageFqName
}