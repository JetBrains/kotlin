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

package org.jetbrains.kotlin.resolve

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.impl.FunctionDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyAccessorDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PropertyDescriptorImpl
import org.jetbrains.kotlin.diagnostics.DiagnosticSink
import org.jetbrains.kotlin.diagnostics.Errors
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.source.getPsi
import org.jetbrains.kotlin.resolve.typeBinding.TypeBinding
import org.jetbrains.kotlin.resolve.typeBinding.createTypeBinding
import org.jetbrains.kotlin.resolve.typeBinding.createTypeBindingForReturnType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.types.Variance.*
import org.jetbrains.kotlin.types.checkTypePosition
import org.jetbrains.kotlin.types.getAbbreviatedType

class ManualVariance(val descriptor: TypeParameterDescriptor, val variance: Variance)

class VarianceChecker(trace: BindingTrace) {
    private val core = VarianceCheckerCore(trace.bindingContext, trace)

    fun check(c: TopDownAnalysisContext) {
        core.check(c)
    }
}

class VarianceConflictDiagnosticData(
        val containingType: KotlinType,
        val typeParameter: TypeParameterDescriptor,
        val occurrencePosition: Variance
)

class VarianceCheckerCore(
        private val context: BindingContext,
        private val diagnosticSink: DiagnosticSink,
        private val manualVariance: ManualVariance? = null
) {

    fun check(c: TopDownAnalysisContext) {
        checkClasses(c)
        checkMembers(c)
    }

    fun checkClassOrObject(klass: KtClassOrObject): Boolean {
        if (klass is KtClass) {
            if (!checkClassHeader(klass)) return false
        }
        for (member in klass.declarations + klass.getPrimaryConstructorParameters()) {
            val descriptor = when (member) {
                is KtParameter -> context.get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, member)
                is KtDeclaration -> context.get(BindingContext.DECLARATION_TO_DESCRIPTOR, member)
                else -> null
            } as? MemberDescriptor ?: continue
            when (member) {
                is KtClassOrObject -> {
                    if (!checkClassOrObject(member)) return false
                }
                is KtCallableDeclaration -> {
                    if (descriptor is CallableMemberDescriptor && !checkMember(member, descriptor)) return false
                }
            }
        }
        return true
    }

    private fun checkClasses(c: TopDownAnalysisContext) {
        for (jetClassOrObject in c.declaredClasses!!.keys) {
            if (jetClassOrObject is KtClass) {
                checkClassHeader(jetClassOrObject)
            }
        }
    }

    private fun checkClassHeader(klass: KtClass): Boolean {
        var noError = true
        for (specifier in klass.getSuperTypeListEntries()) {
            noError = noError and specifier.typeReference?.checkTypePosition(context, OUT_VARIANCE)
        }
        return noError and klass.checkTypeParameters(context, OUT_VARIANCE)
    }

    private fun checkMembers(c: TopDownAnalysisContext) {
        for ((declaration, descriptor) in c.members) {
            checkMember(declaration, descriptor)
        }
    }

    private fun checkMember(member: KtCallableDeclaration, descriptor: CallableMemberDescriptor) =
            Visibilities.isPrivate(descriptor.visibility) || checkCallableDeclaration(context, member, descriptor)

    private fun TypeParameterDescriptor.varianceWithManual() =
            if (manualVariance != null && this.original == manualVariance.descriptor) manualVariance.variance else variance

    fun recordPrivateToThisIfNeeded(descriptor: CallableMemberDescriptor) {
        if (isIrrelevant(descriptor) || descriptor.visibility != Visibilities.PRIVATE) return

        val psiElement = descriptor.source.getPsi() as? KtCallableDeclaration ?: return

        if (!checkCallableDeclaration(context, psiElement, descriptor)) {
            recordPrivateToThis(descriptor)
        }
    }

    private fun checkCallableDeclaration(
            trace: BindingContext,
            declaration: KtCallableDeclaration,
            descriptor: CallableDescriptor
    ): Boolean {
        if (isIrrelevant(descriptor)) return true
        var noError = true

        noError = noError and declaration.checkTypeParameters(trace, IN_VARIANCE)

        noError = noError and declaration.receiverTypeReference?.checkTypePosition(trace, IN_VARIANCE)

        for (parameter in declaration.valueParameters) {
            noError = noError and parameter.typeReference?.checkTypePosition(trace, IN_VARIANCE)
        }

        val returnTypePosition = if (descriptor is VariableDescriptor && descriptor.isVar) INVARIANT else OUT_VARIANCE
        noError = noError and declaration.createTypeBindingForReturnType(trace)?.checkTypePosition(returnTypePosition)

        return noError
    }

    private fun KtTypeParameterListOwner.checkTypeParameters(
            trace: BindingContext,
            typePosition: Variance
    ): Boolean {
        var noError = true
        for (typeParameter in typeParameters) {
            noError = noError and typeParameter.extendsBound?.checkTypePosition(trace, typePosition)
        }
        for (typeConstraint in typeConstraints) {
            noError = noError and typeConstraint.boundTypeReference?.checkTypePosition(trace, typePosition)
        }
        return noError
    }

    private fun KtTypeReference.checkTypePosition(trace: BindingContext, position: Variance)
            = createTypeBinding(trace)?.checkTypePosition(position)

    private fun TypeBinding<PsiElement>.checkTypePosition(position: Variance) = checkTypePosition(type, position)

    private fun TypeBinding<PsiElement>.checkTypePosition(containingType: KotlinType, position: Variance): Boolean =
        checkTypePosition(
                position,
                {   typeParameterDescriptor, typeBinding, errorPosition ->
                    val varianceConflictDiagnosticData = VarianceConflictDiagnosticData(containingType, typeParameterDescriptor, errorPosition)
                    val diagnostic = if (typeBinding.isInAbbreviation) Errors.TYPE_VARIANCE_CONFLICT_IN_EXPANDED_TYPE else Errors.TYPE_VARIANCE_CONFLICT
                    diagnosticSink.report(diagnostic.on(typeBinding.psiElement, varianceConflictDiagnosticData))
                },
                customVariance = { it.varianceWithManual() }
        )

    private fun isIrrelevant(descriptor: CallableDescriptor): Boolean {
        val containingClass = descriptor.containingDeclaration as? ClassDescriptor ?: return true
        return containingClass.typeConstructor.parameters.all { it.varianceWithManual() == INVARIANT }
    }

    companion object {

        private fun recordPrivateToThis(descriptor: CallableMemberDescriptor) {
            when (descriptor) {
                is FunctionDescriptorImpl -> descriptor.visibility = Visibilities.PRIVATE_TO_THIS
                is PropertyDescriptorImpl -> {
                    descriptor.visibility = Visibilities.PRIVATE_TO_THIS
                    for (accessor in descriptor.accessors) {
                        (accessor as PropertyAccessorDescriptorImpl).visibility = Visibilities.PRIVATE_TO_THIS
                    }
                }
                else -> throw IllegalStateException("Unexpected descriptor type: ${descriptor.javaClass.name}")
            }
        }

        private infix fun Boolean.and(other: Boolean?) = if (other == null) this else this and other
    }
}
