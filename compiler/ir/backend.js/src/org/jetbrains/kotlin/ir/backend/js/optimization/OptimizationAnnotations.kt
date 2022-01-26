/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.optimization

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.declarations.IrAnnotationContainer
import org.jetbrains.kotlin.js.backend.ast.JsDocComment
import org.jetbrains.kotlin.js.backend.ast.JsStatement

fun JsStatement.annotate(builder: OptimizationAnnotations.() -> Unit): List<JsStatement> {
    return OptimizationAnnotations()
        .apply { builder() }
        .run { listOf(makeStmt(), this@annotate) }
}

class OptimizationAnnotations {
    private val jsDoc = JsDocComment(mutableMapOf())

    fun constructor() {
        jsDoc.appendTag("constructor")
    }

    fun struct() {
        jsDoc.appendTag("struct")
    }

    fun override() {
        jsDoc.appendTag("override")
    }

    fun final() {
        jsDoc.appendTag("final")
    }

    fun const(type: String? = null) {
        jsDoc.appendTag("const", type)
    }

    fun export() {
        jsDoc.appendTag("export")
    }

    fun public() {
        jsDoc.appendTag("public")
    }

    fun private() {
        jsDoc.appendTag("private")
    }

    fun protected() {
        jsDoc.appendTag("protected")
    }

    fun inheritable(isInheritable: Boolean) {
        if (!isInheritable) {
            final()
        }
    }

    fun exported(isExported: Boolean) {
        if (isExported) {
            export()
        }
    }

    fun visibility(descriptor: DescriptorVisibility) {
        when (descriptor) {
            DescriptorVisibilities.PUBLIC -> public()
            DescriptorVisibilities.PRIVATE -> private()
            DescriptorVisibilities.PROTECTED -> protected()
        }
    }

    internal fun makeStmt(): JsStatement {
        return jsDoc.makeStmt()
    }

    private fun JsDocComment.appendTag(tag: String, tagValue: String? = null) {
        tags[tag] = tagValue
    }
}