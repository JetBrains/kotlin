/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.jsdoc


import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.descriptors.DescriptorVisibility
import org.jetbrains.kotlin.ir.backend.js.utils.JsGenerationContext
import org.jetbrains.kotlin.ir.backend.js.utils.sanitizeName
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.impl.IrSimpleTypeImpl
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.js.backend.ast.*

fun JsStatement.annotateWithContext(context: JsGenerationContext, builder: ExtendedOptimizationAnnotations.() -> Unit): JsStatement {
    return annotate(ExtendedOptimizationAnnotations(context), builder)
}

fun JsStatement.annotateWithoutContext(builder: BasicOptimizationAnnotations.() -> Unit): JsStatement {
    return annotate(BasicOptimizationAnnotations(), builder)
}

private fun <T: BasicOptimizationAnnotations> JsStatement.annotate(annotationsContext: T, builder: T.() -> Unit): JsStatement {
    return annotationsContext
        .apply { builder() }
        .run {
            JsGlobalBlock().apply {
                statements += makeStmt()
                statements += this@annotate
            }
        }
}

open class BasicOptimizationAnnotations {
    private val jsDoc = JsDocComment(mutableMapOf())

    fun constructor() {
        jsDoc.appendTag("constructor")
        jsDoc.appendTag("struct")
    }

    fun interface_() {
        jsDoc.appendTag("interface")
    }

    fun record() {
        jsDoc.appendTag("record")
    }

    fun template(types: Iterable<String>) {
        jsDoc.appendTag("template", description = types.joinToString(", "))
    }

    fun param(type: String, name: String) {
        jsDoc.appendTag("param", type, name)
    }

    fun override() {
        jsDoc.appendTag("override")
    }

    fun final() {
        jsDoc.appendTag("final")
    }

    fun define(type: String) {
        jsDoc.appendTag("define", type)
    }

    fun const(type: String? = null) {
        jsDoc.appendTag("const", type)
    }

    fun type(type: String) {
        jsDoc.appendTag("type", type)
    }

    fun param(type: String) {
        jsDoc.appendTag("param", type)
    }

    fun export() {
        jsDoc.appendTag("export")
    }

    fun extends(type: String) {
        jsDoc.appendTag("extends", type)
    }

    fun implements(type: String) {
        jsDoc.appendTag("implements", type)
    }

    fun returnType(type: String) {
        jsDoc.appendTag("return", type)
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
    internal fun makeStmt(): JsStatement {
        return if (jsDoc.tags.isEmpty()) JsEmpty else jsDoc.makeStmt()
    }

    private fun JsDocComment.appendTag(tag: String, tagValue: String? = null, description: String = "") {
        tags[tag] = tagValue?.run { "{$this} $description" } ?: description
    }
}

class ExtendedOptimizationAnnotations(val context: JsGenerationContext): BasicOptimizationAnnotations() {
    private val typeToClosureTypeAnnotationConverter = IrTypeToJSDocType(context.staticContext.backendContext)

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

    fun mutability(isMutable: Boolean, type: IrType) {
        val closureType = typeToClosureTypeAnnotationConverter.convertOrNull(type)
        if (!isMutable) {
            const(closureType)
        } else if (closureType != null) {
            type(closureType)
        }
    }

    fun exportability(isExported: Boolean) {
        if (isExported) {
            export()
        }
    }

    fun overridden(isOverridden: Boolean) {
        if (isOverridden) {
            override()
        }
    }

    fun withReturnType(type: IrType) {
        if (type == context.staticContext.backendContext.irBuiltIns.unitType) return
        returnType(typeToClosureTypeAnnotationConverter.convert(type))
    }

    fun withParams(params: List<IrValueParameter>) {
        if (params.isEmpty()) return
        params.forEach {
            param(
                typeToClosureTypeAnnotationConverter.convert(it.type),
                it.getName()
            )

        }
    }

    fun withTypeVariables(typeVariables: List<IrTypeParameter>) {
        if (typeVariables.isEmpty()) return
        template(
            typeVariables.map {
                typeToClosureTypeAnnotationConverter.convert(
                    IrSimpleTypeImpl(it.symbol, false, emptyList(), emptyList())
                )
            }
        )
    }

    fun implements(parents: List<IrType>) {
        parents.applyToEachType(::implements)
    }

    fun extends(parents: List<IrType>) {
        parents.applyToEachType(::extends)
    }

    fun constant(type: IrType) {
        typeToClosureTypeAnnotationConverter.convertOrNull(type)?.run {
            define(this)
        }
    }

    private inline fun List<IrType>.applyToEachType(fn: (String) -> Unit) {
        if (isEmpty() || singleOrNull()?.isAny() == true) return
        forEach {
            typeToClosureTypeAnnotationConverter.convertOrNull(it)?.run {
                fn(this)
            }
        }
    }

    private fun IrDeclarationWithName.getName(): String {
        return if (context.localNames != null) {
            context.getNameForValueDeclaration(this).ident
        } else {
            sanitizeName(name.asString())
        }
    }
}
