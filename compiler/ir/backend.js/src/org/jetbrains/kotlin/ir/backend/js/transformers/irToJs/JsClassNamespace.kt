/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.transformers.irToJs

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.lower.CallableReferenceLowering
import org.jetbrains.kotlin.ir.backend.js.utils.sanitizeName
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.util.fqNameWhenAvailable
import org.jetbrains.kotlin.ir.util.isLocal
import org.jetbrains.kotlin.js.backend.ast.*
import org.jetbrains.kotlin.name.parentOrNull
import org.jetbrains.kotlin.utils.addToStdlib.popLast

private fun String.makeNamespaceVarName(): JsName {
    val varName = "ns$" + sanitizeName(this)
    return JsName(varName, true)
}

data class ClassNamespace(val namespace: String, val variableName: JsName)

internal fun generateNamespaceVar(klass: IrClass, backendContext: JsIrBackendContext): ClassNamespace? {
    if (klass.isLocal) {
        return null
    }
    val qualifiedName = backendContext.classOriginalFqNames[klass] ?: klass.fqNameWhenAvailable ?: return null
    if (qualifiedName.pathSegments().any { it.isSpecial }) {
        return null
    }
    val namespaceFqName = qualifiedName.parentOrNull() ?: return null
    if (namespaceFqName.isRoot) {
        return null
    }
    val classNamespace = namespaceFqName.toString()
    val namespaceName = classNamespace.makeNamespaceVarName()
    return ClassNamespace(classNamespace, namespaceName)
}

private class NamespaceNode(
    var variableName: JsName?,
    var namespacePart: String,
    var fullNamespacePath: String,
    var parent: NamespaceNode?,
    var kids: MutableMap<String, NamespaceNode>
)

internal fun buildNamespaceVarsInitialization(namespaces: List<ClassNamespace>): JsVars {
    val namespaceTreeRoot = NamespaceNode(null, "", "", null, mutableMapOf())

    // build a tree from namespace parts
    for ((namespace, variableName) in namespaces) {
        var parentNode = namespaceTreeRoot
        for (part in namespace.split('.')) {
            parentNode = parentNode.kids.getOrPut(part) {
                val fullPath = if (parentNode.fullNamespacePath.isEmpty()) part else "${parentNode.fullNamespacePath}.$part"
                NamespaceNode(null, part, fullPath, parentNode, mutableMapOf())
            }
        }
        // It is expected that the merger has renamed all variables before (in Merger::linkJsNames()).
        // Therefore, the same namespaces must have the same variable names.
        if (parentNode.variableName != null && parentNode.variableName !== variableName) {
            error("namespace '$namespace' associated with different variables: '$variableName' and '${parentNode.variableName}'")
        }
        parentNode.variableName = variableName
    }

    val initializationList = mutableListOf<JsVars.JsVar>()

    val processingNodes = namespaceTreeRoot.kids.values.toMutableList()
    while (processingNodes.isNotEmpty()) {
        val node = processingNodes.popLast()

        // merge paths if possible
        while (node.variableName == null && node.kids.size == 1) {
            val kid = node.kids.values.single()
            node.variableName = kid.variableName
            node.namespacePart = "${node.namespacePart}.${kid.namespacePart}"
            node.fullNamespacePath = kid.fullNamespacePath
            node.kids = kid.kids
            node.kids.values.forEach { it.parent = node }
        }

        if (node.variableName == null) {
            node.variableName = node.fullNamespacePath.makeNamespaceVarName()
        }
        val initExpression = if (node.parent === namespaceTreeRoot) {
            JsStringLiteral(node.namespacePart)
        } else {
            JsBinaryOperation(JsBinaryOperator.ADD, node.parent!!.variableName!!.makeRef(), JsStringLiteral(".${node.namespacePart}"))
        }
        initializationList += JsVars.JsVar(node.variableName, initExpression)
        processingNodes += node.kids.values
    }

    return JsVars(initializationList, true)
}
