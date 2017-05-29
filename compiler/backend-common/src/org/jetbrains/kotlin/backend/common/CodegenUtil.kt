/*
 * Copyright 2010-2015 JetBrains s.r.o.
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

package org.jetbrains.kotlin.backend.common

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.backend.common.bridges.findInterfaceImplementation
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.DescriptorUtils
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
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
    fun getNonPrivateTraitMethods(descriptor: ClassDescriptor): Map<FunctionDescriptor, FunctionDescriptor> {
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
            result.putAll(copyFunctions(declaration, traitMember, declaration.containingDeclaration, traitMember.modality,
                                        Visibilities.PUBLIC, CallableMemberDescriptor.Kind.DECLARATION, true))
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
    ): Map<FunctionDescriptor, FunctionDescriptor> {
        val copy = inheritedMember.copy(newOwner, modality, visibility, kind, copyOverrides)
        val result = linkedMapOf<FunctionDescriptor, FunctionDescriptor>()
        if (traitMember is SimpleFunctionDescriptor) {
            result[traitMember] = copy as FunctionDescriptor
        }
        else if (traitMember is PropertyDescriptor) {
            for (traitAccessor in traitMember.accessors) {
                for (inheritedAccessor in (copy as PropertyDescriptor).accessors) {
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
                        ?: error("superType should not be null: ${specifier.text}")

        return superType.constructor.declarationDescriptor as? ClassDescriptor
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
    fun BindingContext.isExhaustive(whenExpression: KtWhenExpression, isStatement: Boolean): Boolean {
        val slice = if (isStatement && !whenExpression.isUsedAsExpression(this)) {
            BindingContext.IMPLICIT_EXHAUSTIVE_WHEN
        }
        else {
            BindingContext.EXHAUSTIVE_WHEN
        }
        return this[slice, whenExpression] == true
    }

    @JvmStatic
    fun constructFakeFunctionCall(project: Project, referencedFunction: FunctionDescriptor): KtCallExpression {
        val fakeFunctionCall = StringBuilder("callableReferenceFakeCall(")
        fakeFunctionCall.append(referencedFunction.valueParameters.map { "p${it.index}" }.joinToString(", "))
        fakeFunctionCall.append(")")
        return KtPsiFactory(project, markGenerated = false).createExpression(fakeFunctionCall.toString()) as KtCallExpression
    }
}
