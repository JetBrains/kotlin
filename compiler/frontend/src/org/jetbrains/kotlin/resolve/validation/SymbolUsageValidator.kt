package org.jetbrains.kotlin.resolve.validation

import com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.resolve.BindingTrace
import kotlin.platform.platformStatic

public trait SymbolUsageValidator {

    public fun validateTypeUsage(targetDescriptor: ClassifierDescriptor, trace: BindingTrace, element: PsiElement) { }

    public fun validateCall(targetDescriptor: CallableDescriptor, trace: BindingTrace, element: PsiElement) { }

    public open class Composite(val validators: List<SymbolUsageValidator>) : SymbolUsageValidator {
        override fun validateCall(targetDescriptor: CallableDescriptor, trace: BindingTrace, element: PsiElement) {
            validators.forEach { it.validateCall(targetDescriptor, trace, element) }
        }

        override fun validateTypeUsage(targetDescriptor: ClassifierDescriptor, trace: BindingTrace, element: PsiElement) {
            validators.forEach { it.validateTypeUsage(targetDescriptor, trace, element) }
        }
    }

    companion object {
        val Empty: SymbolUsageValidator = Composite(listOf())
    }
}