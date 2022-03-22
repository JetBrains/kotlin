/*
 * Copyright 2010-2022 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.jsdoc

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext
import org.jetbrains.kotlin.ir.backend.js.export.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.js.common.isValidES5Identifier

class IrTypeToJSDocType(context: JsIrBackendContext) {
    private val exportModelGenerator = ExportModelGenerator(context, false)

    fun convert(type: IrType): String {
        return exportModelGenerator
            .exportType(type, withImplicitExport = false)
            .toClosureTypeAnnotation("")
    }

    fun convertOrNull(type: IrType): String? {
        return convert(type)
            .takeIf { !isUnknownType(it) }
    }

    fun isUnknownType(type: String): Boolean {
        return type == "?"
    }

    private fun ExportedType.toClosureTypeAnnotation(indent: String): String {
        return when (this) {
            is ExportedType.ErrorType,
            is ExportedType.Primitive.Any,
            is ExportedType.Primitive.Nothing,
            is ExportedType.ImplicitlyExportedType -> "?"
            is ExportedType.Primitive.Unit -> "Unit"
            is ExportedType.LiteralType.StringLiteralType -> "string"
            is ExportedType.LiteralType.NumberLiteralType -> "number"
            is ExportedType.TypeParameter -> name
            is ExportedType.Primitive -> typescript
            // TODO: Think about intersection type in Closure Compiler
            is ExportedType.IntersectionType -> lhs.toClosureTypeAnnotation(indent)
            is ExportedType.TypeOf -> "typeof $name"
            is ExportedType.UnionType -> "(${toUnionList(indent)})"
            is ExportedType.Array -> "Array<${elementType.toClosureTypeAnnotation(indent)}>"
            is ExportedType.Nullable -> ExportedType.UnionType(
                baseType,
                ExportedType.UnionType(ExportedType.TypeParameter("undefined"), ExportedType.TypeParameter("null"))
            ).toClosureTypeAnnotation(indent)
            is ExportedType.ClassType ->
                name + if (arguments.isNotEmpty()) "<${arguments.joinToString(", ") { it.toClosureTypeAnnotation(indent) }}>" else ""
            is ExportedType.InlineInterfaceType -> {
                members
                    .mapNotNull { it.toClosureTypeAnnotation("$indent    ") }
                    .joinToString(prefix = "{\n", postfix = "$indent}", separator = ",\n")
            }
            is ExportedType.Function ->
                parameterTypes
                    .joinToString(", ") { it.toClosureTypeAnnotation(indent) }
                    .run { "function($this)${returnType.toClosureReturnTypeAnnotation(indent)}" }
        }
    }

    private fun ExportedType.toUnionList(indent: String): String {
        return when (this) {
            is ExportedType.UnionType -> lhs.toUnionList(indent) + "|" + rhs.toUnionList(indent)
            else -> toClosureTypeAnnotation(indent)
        }
    }

    private fun ExportedType.toClosureReturnTypeAnnotation(indent: String): String {
        return when (this) {
            is ExportedType.Primitive.Unit -> ""
            else -> ": ${toClosureTypeAnnotation(indent)}"
        }
    }


    private fun ExportedDeclaration.toClosureTypeAnnotation(indent: String): String? = indent + when (this) {
        is ExportedClass -> if (name.isValidES5Identifier()) name else ""
        is ExportedFunction -> {
            val containsUnresolvedChar = !name.isValidES5Identifier()
            val type = ExportedType.Function(parameters.map { it.type }, returnType).toClosureTypeAnnotation(indent)

            val escapedName = when {
                isMember && containsUnresolvedChar -> "\"$name\""
                else -> name
            }

            "$escapedName: $type"
        }

        is ExportedConstructor,
        is ExportedConstructSignature -> {
            val parameterList = when (this) {
                is ExportedConstructor -> parameters
                is ExportedConstructSignature -> parameters
                else -> error("Expect this type to be ExportedConstructor or ExportedConstructSignature")
            }
            val returnType = when (this) {
                is ExportedConstructor -> ExportedType.Primitive.Any
                is ExportedConstructSignature -> returnType
                else -> error("Expect this type to be ExportedConstructor or ExportedConstructSignature")
            }
            "function(new: ${returnType.toClosureTypeAnnotation(indent)}, ${
                parameterList.joinToString(", ") {
                    it.type.toClosureTypeAnnotation(
                        indent
                    )
                }
            })"
        }

        is ExportedProperty -> {
            val containsUnresolvedChar = !name.isValidES5Identifier()
            val memberName = when {
                isMember && containsUnresolvedChar -> "\"$name\""
                else -> name
            }
            val typeString = type.toClosureTypeAnnotation(indent)
            if (typeString == "?") memberName else "$memberName: $typeString"
        }

        else -> null
    }
}