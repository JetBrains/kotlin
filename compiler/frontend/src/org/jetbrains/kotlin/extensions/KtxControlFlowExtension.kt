package org.jetbrains.kotlin.extensions

import org.jetbrains.kotlin.cfg.ControlFlowBuilder
import org.jetbrains.kotlin.psi.KtVisitorVoid
import org.jetbrains.kotlin.psi.KtxElement
import org.jetbrains.kotlin.resolve.BindingTrace

interface KtxControlFlowExtension {
    companion object : ProjectExtensionDescriptor<KtxControlFlowExtension>(
        "org.jetbrains.kotlin.ktxControlFlowExtension",
        KtxControlFlowExtension::class.java
    )

    fun visitKtxElement(
        element: KtxElement,
        builder: ControlFlowBuilder,
        visitor: KtVisitorVoid,
        trace: BindingTrace
    )
}