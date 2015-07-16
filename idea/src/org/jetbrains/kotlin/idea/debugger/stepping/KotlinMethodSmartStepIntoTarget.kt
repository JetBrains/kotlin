package org.jetbrains.kotlin.idea.debugger.stepping

import com.intellij.debugger.actions.SmartStepTarget
import com.intellij.psi.PsiElement
import com.intellij.util.Range
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.psi.JetElement

public class KotlinMethodSmartStepTarget(
        val resolvedElement: JetElement,
        label: String,
        highlightElement: PsiElement,
        lines: Range<Int>
): SmartStepTarget(label, highlightElement, false, lines) {
    companion object {
        fun calcLabel(descriptor: DeclarationDescriptor): String {
            return descriptor.getName().asString()
        }
    }
}