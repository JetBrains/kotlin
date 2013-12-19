package org.jetbrains.jet.plugin.completion

import org.jetbrains.jet.lang.resolve.calls.autocasts.DataFlowValue
import org.jetbrains.jet.lang.resolve.scopes.receivers.ThisReceiver
import org.jetbrains.jet.lang.psi.JetExpression
import org.jetbrains.jet.lang.descriptors.VariableDescriptor
import org.jetbrains.jet.lang.descriptors.PackageViewDescriptor

fun renderDataFlowValue(value: DataFlowValue): String? {
    // If it is not a stable identifier, there's no point in rendering it
    if (!value.isStableIdentifier()) return null

    fun renderId(id: Any?): String? {
        return when (id) {
            is JetExpression -> id.getText()
            is ThisReceiver -> "this@${id.getDeclarationDescriptor().getName()}"
            is VariableDescriptor -> id.getName().asString()
            is PackageViewDescriptor -> id.getFqName().asString()
            is com.intellij.openapi.util.Pair<*, *> -> renderId(id.first) + "." + renderId(id.second)
            else -> null
        }
    }
    return renderId(value.getId())
}