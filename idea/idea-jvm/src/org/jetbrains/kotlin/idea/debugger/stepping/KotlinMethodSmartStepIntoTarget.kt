package org.jetbrains.kotlin.idea.debugger.stepping

import com.intellij.debugger.actions.SmartStepTarget
import com.intellij.psi.PsiElement
import com.intellij.util.Range
import org.jetbrains.kotlin.builtins.functions.FunctionInvokeDescriptor
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.idea.KotlinIcons
import org.jetbrains.kotlin.idea.util.IdeDescriptorRenderers
import org.jetbrains.kotlin.load.java.JvmAbi
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.renderer.ParameterNameRenderingPolicy
import org.jetbrains.kotlin.resolve.descriptorUtil.isExtension
import javax.swing.Icon

class KotlinMethodSmartStepTarget(
    descriptor: CallableMemberDescriptor,
    val declaration: KtDeclaration?,
    label: String,
    highlightElement: PsiElement,
    lines: Range<Int>
) : SmartStepTarget(label, highlightElement, false, lines) {
    val isInvoke = descriptor is FunctionInvokeDescriptor

    init {
        assert(declaration != null || isInvoke)
    }

    private val isExtension = descriptor.isExtension

    val targetMethodName: String = when (descriptor) {
        is ClassDescriptor, is ConstructorDescriptor -> "<init>"
        is PropertyAccessorDescriptor -> JvmAbi.getterName(descriptor.correspondingProperty.name.asString())
        else -> descriptor.name.asString()
    }

    override fun getIcon(): Icon? {
        return when {
            isExtension -> KotlinIcons.EXTENSION_FUNCTION
            else -> KotlinIcons.FUNCTION
        }
    }

    companion object {
        private val renderer = IdeDescriptorRenderers.SOURCE_CODE_SHORT_NAMES_NO_ANNOTATIONS.withOptions {
            parameterNameRenderingPolicy = ParameterNameRenderingPolicy.NONE
            withoutReturnType = true
            renderAccessors = true
            startFromName = true
            modifiers = emptySet()
        }

        fun calcLabel(descriptor: DeclarationDescriptor): String {
            return renderer.render(descriptor)
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true

        if (other == null || other !is KotlinMethodSmartStepTarget) return false

        if (isInvoke && other.isInvoke) {
            // Don't allow to choose several invoke targets in smart step into as we can't distinguish them reliably during debug
            return true
        }

        return declaration === other.declaration
    }

    override fun hashCode(): Int {
        if (isInvoke) {
            // Predefined value to make all FunctionInvokeDescriptor targets equal
            return 42
        }
        return declaration!!.hashCode()
    }
}