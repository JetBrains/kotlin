/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.export

import org.jetbrains.kotlin.ir.backend.js.utils.sanitizeName
import org.jetbrains.kotlin.serialization.js.ModuleKind

// TODO: Support module kinds other than plain

fun ExportedModule.toTypeScript(): String {
    val indent = if (moduleKind == ModuleKind.PLAIN) "    " else ""
    val types = "${indent}type Nullable<T> = T | null | undefined\n"

    val declarationsDts =
        types + declarations.joinToString("\n") {
            it.toTypeScript(
                indent = indent,
                prefix = if (moduleKind == ModuleKind.PLAIN) "" else "export "
            )
        }

    val namespaceName = sanitizeName(name)

    return when (moduleKind) {
        ModuleKind.PLAIN -> "declare namespace $namespaceName {\n$declarationsDts\n}\n"
        ModuleKind.AMD, ModuleKind.COMMON_JS -> declarationsDts
        ModuleKind.UMD -> "$declarationsDts\nexport as namespace $namespaceName;"
    }
}

fun List<ExportedDeclaration>.toTypeScript(indent: String): String =
    joinToString("") { it.toTypeScript(indent) + "\n" }

fun ExportedDeclaration.toTypeScript(indent: String, prefix: String = ""): String = indent + when (this) {
    is ErrorDeclaration -> "/* ErrorDeclaration: $message */"

    is ExportedNamespace ->
        "${prefix}namespace $name {\n" + declarations.toTypeScript("$indent    ") + "$indent}"

    is ExportedFunction -> {
        val keyword: String = when {
            isMember -> when {
                isStatic -> "static "
                isAbstract -> "abstract "
                else -> ""
            }
            else -> "function "
        }

        val renderedParameters = parameters.joinToString(", ") { it.toTypeScript(indent) }

        val renderedTypeParameters =
            if (typeParameters.isNotEmpty())
                "<" + typeParameters.joinToString(", ") + ">"
            else
                ""

        val renderedReturnType = returnType.toTypeScript(indent)

        "${prefix}$keyword$name$renderedTypeParameters($renderedParameters): $renderedReturnType;"
    }
    is ExportedConstructor ->
        "constructor(${parameters.joinToString(", ") { it.toTypeScript(indent) }});"

    is ExportedProperty -> {
        val keyword = when {
            isMember -> (if (isAbstract) "abstract " else "") + (if (!mutable) "readonly " else "")
            else -> if (mutable) "let " else "const "
        }
        prefix + keyword + name + ": " + type.toTypeScript(indent) + ";"
    }

    is ExportedClass -> {
        val keyword = if (isInterface) "interface" else "class"
        val superInterfacesKeyword = if (isInterface) "extends" else "implements"

        val superClassClause = superClass?.let { " extends ${it.toTypeScript(indent)}" } ?: ""
        val superInterfacesClause = if (superInterfaces.isNotEmpty()) {
            " $superInterfacesKeyword " + superInterfaces.joinToString(", ") { it.toTypeScript(indent) }
        } else ""

        val membersString = members.joinToString("") { it.toTypeScript("$indent    ") + "\n" }

        // If there are no exported constructors, add a private constructor to disable default one
        val privateCtorString =
            if (!isInterface && !isAbstract && members.none { it is ExportedConstructor })
                "$indent    private constructor();\n"
            else
                ""

        val renderedTypeParameters =
            if (typeParameters.isNotEmpty())
                "<" + typeParameters.joinToString(", ") + ">"
            else
                ""

        val modifiers = if (isAbstract && !isInterface) "abstract " else ""

        val bodyString = privateCtorString + membersString + indent

        val klassExport = "$prefix$modifiers$keyword $name$renderedTypeParameters$superClassClause$superInterfacesClause {\n$bodyString}"
        val staticsExport = if (nestedClasses.isNotEmpty()) "\n" + ExportedNamespace(name, nestedClasses).toTypeScript(indent, prefix) else ""
        klassExport + staticsExport
    }
}

fun ExportedParameter.toTypeScript(indent: String): String =
    "$name: ${type.toTypeScript(indent)}"

fun ExportedType.toTypeScript(indent: String): String = when (this) {
    is ExportedType.Primitive -> typescript
    is ExportedType.Array -> "Array<${elementType.toTypeScript(indent)}>"
    is ExportedType.Function -> "(" + parameterTypes
        .withIndex()
        .joinToString(", ") { (index, type) ->
            "p$index: ${type.toTypeScript(indent)}"
        } + ") => " + returnType.toTypeScript(indent)

    is ExportedType.ClassType ->
        name + if (arguments.isNotEmpty()) "<${arguments.joinToString(", ") { it.toTypeScript(indent) }}>" else ""
    is ExportedType.TypeOf ->
        "typeof $name"

    is ExportedType.ErrorType -> "any /*$comment*/"
    is ExportedType.TypeParameter -> name
    is ExportedType.Nullable -> "Nullable<" + baseType.toTypeScript(indent) + ">"
    is ExportedType.InlineInterfaceType -> {
        members.joinToString(prefix = "{\n", postfix = "$indent}", separator = "") { it.toTypeScript("$indent    ") + "\n" }
    }
    is ExportedType.IntersectionType -> {
        lhs.toTypeScript(indent) + " & " + rhs.toTypeScript(indent)
    }
}