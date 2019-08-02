/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

import org.jetbrains.kotlin.ir.declarations.*

sealed class ExportedDeclaration

class ExportedNamespace(
    val name: String,
    val declarations: List<ExportedDeclaration>
) : ExportedDeclaration()

class ExportedFunction(
    val name: String,
    val returnType: ExportedType,
    val parameters: List<ExportedParameter>,
    val typeParameters: List<String> = emptyList(),
    val isMember: Boolean = false,
    val isStatic: Boolean = false,
    val isAbstract: Boolean = false,
    val ir: IrSimpleFunction
) : ExportedDeclaration()

class ExportedConstructor(
    val parameters: List<ExportedParameter>,
    val isPrivate: Boolean
) : ExportedDeclaration()

class ExportedProperty(
    val name: String,
    val type: ExportedType,
    val mutable: Boolean,
    val isMember: Boolean = false,
    val isStatic: Boolean = false,
    val ir: IrProperty
) : ExportedDeclaration()

class ErrorDeclaration(val message: String) : ExportedDeclaration()

class ExportedClass(
    val name: String,
    val isInterface: Boolean = false,
    val isAbstract: Boolean = false,
    val superClass: ExportedType? = null,
    val superInterfaces: List<ExportedType> = emptyList(),
    val typeParameters: List<String>,
    val members: List<ExportedDeclaration>,
    val statics: List<ExportedDeclaration>,
    val ir: IrClass
) : ExportedDeclaration()

class ExportedParameter(
    val name: String,
    val type: ExportedType
)

sealed class ExportedType {
    sealed class Primitive(val typescript: kotlin.String) : ExportedType() {
        object Boolean : Primitive("boolean")
        object Number : Primitive("number")
        object ByteArray : Primitive("Int8Array")
        object ShortArray : Primitive("Int16Array")
        object IntArray : Primitive("Int32Array")
        object FloatArray : Primitive("Float32Array")
        object DoubleArray : Primitive("Float64Array")
        object String : Primitive("string")
        object Throwable : Primitive("Error")
        object Any : Primitive("any")
        object Unit : Primitive("void")
        object Nothing : Primitive("never")
    }

    class Array(val elementType: ExportedType) : ExportedType()
    class Function(
        val parameterTypes: List<ExportedType>,
        val returnType: ExportedType
    ) : ExportedType()

    class ClassType(val name: String, val arguments: List<ExportedType>) : ExportedType()
    class TypeParameter(val name: String) : ExportedType()
    class Nullable(val baseType: ExportedType) : ExportedType()
    class ErrorType(val comment: String) : ExportedType()

    fun withNullability(nullable: Boolean) =
        if (nullable) Nullable(this) else this
}

data class ExportedModule(
    val name: String,
    val declarations: List<ExportedDeclaration>
)

fun ExportedModule.toTypeScript(): String {
    val prefix = "    type Nullable<T> = T | null | undefined\n"
    val body = declarations.joinToString("\n") { it.toTypeScript("    ", "") }
    return "declare namespace $name {\n$prefix\n$body\n}\n"
}

fun List<ExportedDeclaration>.toTypeScript(ident: String, exportModifier: String = ""): String =
    joinToString("\n") { it.toTypeScript(ident, exportModifier) }

fun ExportedDeclaration.toTypeScript(ident: String, exportModifier: String = ""): String = ident + when (this) {
    is ErrorDeclaration -> "namespace _Error_ { /* $message */ }"

    is ExportedNamespace ->
        exportModifier + "namespace $name {\n" + declarations.toTypeScript("$ident    ") + "$ident}\n"

    is ExportedFunction -> {
        val keyword: String = when {
            isMember -> when {
                isStatic -> "static "
                isAbstract -> "abstract "
                else -> ""
            }
            else -> "function "
        }

        val renderedParameters = parameters.joinToString(", ") { it.toTypeScript() }

        val renderedTypeParameters =
            if (typeParameters.isNotEmpty())
                "<" + typeParameters.joinToString(", ") + ">"
            else
                ""

        val renderedReturnType = returnType.toTypeScript()

        exportModifier + "$keyword$name$renderedTypeParameters($renderedParameters): $renderedReturnType\n"
    }
    is ExportedConstructor ->
        "constructor(${parameters.joinToString(", ") { it.toTypeScript() }})\n"

    is ExportedProperty -> {
        val keyword = when {
            isMember -> if (!mutable) "readonly " else ""
            else -> if (mutable) "let " else "const "
        }
        exportModifier + keyword + name + ": " + type.toTypeScript() + ";\n"
    }

    is ExportedClass -> {
        val keyword = if (isInterface) "interface" else "class"
        val superInterfacesKeyword = if (isInterface) "extends" else "implements"

        val superClassClause = superClass?.let { " extends ${it.toTypeScript()}" } ?: ""
        val superInterfacesClause = if (superInterfaces.isNotEmpty()) {
            " $superInterfacesKeyword " + superInterfaces.joinToString(", ") { it.toTypeScript() }
        } else ""

        val membersString = members.joinToString("\n") { it.toTypeScript("$ident    ") }

        val renderedTypeParameters =
            if (typeParameters.isNotEmpty())
                "<" + typeParameters.joinToString(", ") + ">"
            else
                ""

        val modifiers = if (isAbstract && !isInterface) "abstract " else ""

        val klassExport = exportModifier + "$modifiers$keyword $name$renderedTypeParameters$superClassClause$superInterfacesClause {\n$membersString$ident}\n"
        val staticsExport = if (statics.isNotEmpty()) {
            "\n" + exportModifier + ident +  ExportedNamespace(name, statics).toTypeScript(ident)
        } else ""
        klassExport + staticsExport
    }
}

fun ExportedParameter.toTypeScript(): String =
    "$name: ${type.toTypeScript()}"

fun ExportedType.toTypeScript(): String = when (this) {
    is ExportedType.Primitive -> typescript
    is ExportedType.Array -> "Array<${elementType.toTypeScript()}>"
    is ExportedType.Function -> "(" + parameterTypes
        .withIndex()
        .joinToString(", ") { (index, type) ->
            "arg$index: ${type.toTypeScript()}"
        } + ") => " + returnType.toTypeScript()

    is ExportedType.ClassType ->
        name + if (arguments.isNotEmpty()) "<${arguments.joinToString(", ") { it.toTypeScript() }}>" else ""

    is ExportedType.ErrorType -> "any /*$comment*/"
    is ExportedType.TypeParameter -> name
    is ExportedType.Nullable -> "Nullable<" + baseType.toTypeScript() + ">"
}