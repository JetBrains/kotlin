package org.jetbrains.kotlin.resolve.validation

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall

public open class CompositeSymbolUsageValidator(val validators: List<SymbolUsageValidator>) : SymbolUsageValidator {

    constructor(vararg validator : SymbolUsageValidator) : this(validator.toList()) {}

    override fun validateCall(targetDescriptor: CallableDescriptor, trace: BindingTrace, element: PsiElement) {
        validators.forEach { it.validateCall(targetDescriptor, trace, element) }
    }

    override fun validateTypeUsage(targetDescriptor: ClassifierDescriptor, trace: BindingTrace, element: PsiElement) {
        validators.forEach { it.validateTypeUsage(targetDescriptor, trace, element) }
    }
}