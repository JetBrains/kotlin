/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.export

import org.jetbrains.kotlin.ir.backend.js.utils.getJsNameOrKotlinName
import org.jetbrains.kotlin.ir.backend.js.utils.sanitizeName
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.js.common.isValidES5Identifier
import org.jetbrains.kotlin.serialization.js.ModuleKind

// TODO: Support module kinds other than plain

fun ExportedModule.toTypeScript(): String {
    return wrapTypeScript(name, moduleKind, declarations.toTypeScript(moduleKind))
}

fun wrapTypeScript(name: String, moduleKind: ModuleKind, dts: String): String {
    val types = "${moduleKind.indent}type Nullable<T> = T | null | undefined\n"

    val declarationsDts = types + dts

    val namespaceName = sanitizeName(name, withHash = false)

    return when (moduleKind) {
        ModuleKind.PLAIN -> "declare namespace $namespaceName {\n$declarationsDts\n}\n"
        ModuleKind.AMD, ModuleKind.COMMON_JS, ModuleKind.ES -> declarationsDts
        ModuleKind.UMD -> "$declarationsDts\nexport as namespace $namespaceName;"
    }
}

private val ModuleKind.indent: String
    get() = if (this == ModuleKind.PLAIN) "    " else ""

fun List<ExportedDeclaration>.toTypeScript(moduleKind: ModuleKind): String {
    return joinToString("\n") {
        it.toTypeScript(
            indent = moduleKind.indent,
            prefix = if (moduleKind == ModuleKind.PLAIN) "" else "export "
        )
    }
}

fun List<ExportedDeclaration>.toTypeScript(indent: String): String =
    joinToString("") { it.toTypeScript(indent) + "\n" }

fun ExportedDeclaration.toTypeScript(indent: String, prefix: String = ""): String = indent + when (this) {
    is ErrorDeclaration -> "/* ErrorDeclaration: $message */"

    is ExportedNamespace ->
        "${prefix}namespace $name {\n" + declarations.toTypeScript("$indent    ") + "$indent}"

    is ExportedFunction -> {
        val visibility = if (isProtected) "protected " else ""

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
        val containsUnresolvedChar = !name.isValidES5Identifier()

        val escapedName = when {
            isMember && containsUnresolvedChar -> "\"$name\""
            else -> name
        }

        if (!isMember && containsUnresolvedChar) "" else "${prefix}$visibility$keyword$escapedName$renderedTypeParameters($renderedParameters): $renderedReturnType;"
    }

    is ExportedConstructor -> {
        val visibility = if (isProtected) "protected " else ""
        val renderedParameters = parameters.joinToString(", ") { it.toTypeScript(indent) }
        "${visibility}constructor($renderedParameters);"
    }

    is ExportedConstructSignature -> {
        val renderedParameters = parameters.joinToString(", ") { it.toTypeScript(indent) }
        "new($renderedParameters): ${returnType.toTypeScript(indent)};"
    }

    is ExportedProperty -> {
        val visibility = if (isProtected) "protected " else ""
        val keyword = when {
            isMember -> (if (isAbstract) "abstract " else "") + (if (!mutable) "readonly " else "")
            else -> if (mutable) "let " else "const "
        }
        val possibleStatic = if (isMember && isStatic) "static " else ""
        val containsUnresolvedChar = !name.isValidES5Identifier()
        val memberName = when {
            isMember && containsUnresolvedChar -> "\"$name\""
            else -> name
        }
        if (!isMember && containsUnresolvedChar) "" else "$prefix$visibility$possibleStatic$keyword$memberName: ${type.toTypeScript(indent)};"
    }

    is ExportedClass -> {
        val keyword = if (isInterface) "interface" else "class"
        val superInterfacesKeyword = if (isInterface) "extends" else "implements"

        val superClassClause = superClass?.let { " extends ${it.toTypeScript(indent)}" } ?: ""
        val superInterfacesClause = if (superInterfaces.isNotEmpty()) {
            " $superInterfacesKeyword " + superInterfaces.joinToString(", ") { it.toTypeScript(indent) }
        } else ""

        val members = members.map {
            if (!ir.isInner || it !is ExportedFunction || !it.isStatic) {
                it
            } else {
                // Remove $outer argument from secondary constructors of inner classes
                it.copy(parameters = it.parameters.drop(1))
            }
        }

        val (innerClasses, nonInnerClasses) = nestedClasses.partition { it.ir.isInner }
        val innerClassesProperties = innerClasses.map { it.toReadonlyProperty() }
        val membersString = (members + innerClassesProperties).joinToString("") { it.toTypeScript("$indent    ") + "\n" }

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

        val nestedClasses = nonInnerClasses + innerClasses.map { it.withProtectedConstructors() }
        val klassExport = "$prefix$modifiers$keyword $name$renderedTypeParameters$superClassClause$superInterfacesClause {\n$bodyString}"
        val staticsExport = if (nestedClasses.isNotEmpty()) "\n" + ExportedNamespace(name, nestedClasses).toTypeScript(indent, prefix) else ""

        if (name.isValidES5Identifier()) klassExport + staticsExport else ""
    }
}

fun IrClass.asNestedClassAccess(): String {
    val name = getJsNameOrKotlinName().identifier
    if (parent !is IrClass) return name
    return "${parentAsClass.asNestedClassAccess()}.$name"
}

fun ExportedClass.withProtectedConstructors(): ExportedClass {
    return copy(members = members.map {
        if (it !is ExportedConstructor || it.isProtected) {
            it
        } else {
            it.copy(isProtected = true)
        }
    })
}

fun ExportedClass.toReadonlyProperty(): ExportedProperty {
    val innerClassReference = ir.asNestedClassAccess()
    val allPublicConstructors = members.asSequence()
        .filterIsInstance<ExportedConstructor>()
        .filterNot { it.isProtected }
        .map {
            ExportedConstructSignature(
                parameters = it.parameters.drop(1),
                returnType = ExportedType.TypeParameter(innerClassReference),
            )
        }
        .toList()

    val type = ExportedType.IntersectionType(
        ExportedType.InlineInterfaceType(allPublicConstructors),
        ExportedType.TypeOf(innerClassReference)
    )

    return ExportedProperty(
        name = name,
        type = type,
        mutable = false,
        isMember = true,
        isStatic = false,
        isAbstract = false,
        isProtected = false,
        irGetter = null,
        irSetter = null
    )
}

fun ExportedParameter.toTypeScript(indent: String): String =
    "${sanitizeName(name, withHash = false)}: ${type.toTypeScript(indent)}"

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

    is ExportedType.TypeParameter -> name
    is ExportedType.ErrorType -> "any /*$comment*/"
    is ExportedType.Nullable -> "Nullable<" + baseType.toTypeScript(indent) + ">"
    is ExportedType.InlineInterfaceType -> {
        members.joinToString(prefix = "{\n", postfix = "$indent}", separator = "") { it.toTypeScript("$indent    ") + "\n" }
    }
    is ExportedType.IntersectionType -> {
        lhs.toTypeScript(indent) + " & " + rhs.toTypeScript(indent)
    }
    is ExportedType.UnionType -> {
        lhs.toTypeScript(indent) + " | " + rhs.toTypeScript(indent)
    }
    is ExportedType.LiteralType.StringLiteralType -> "\"$value\""
    is ExportedType.LiteralType.NumberLiteralType -> value.toString()
}