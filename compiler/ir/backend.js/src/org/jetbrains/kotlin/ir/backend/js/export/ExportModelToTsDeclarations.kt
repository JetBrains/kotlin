/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.export

import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.backend.js.utils.getFqNameWithJsNameWhenAvailable
import org.jetbrains.kotlin.ir.backend.js.utils.getJsNameOrKotlinName
import org.jetbrains.kotlin.ir.backend.js.utils.sanitizeName
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.js.common.isValidES5Identifier
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.serialization.js.ModuleKind

const val Nullable = "Nullable"
const val doNotImplementIt = "__doNotImplementIt"
const val objectInstances = "__object_instances__"
const val Instance = "INSTANCE"
// TODO: Support module kinds other than plain

fun ExportedModule.toTypeScript(): String {
    return ExportModelToTsDeclarations().generateTypeScriptFor(name, moduleKind, this)
}

fun List<ExportedDeclaration>.toTypeScript(moduleKind: ModuleKind): String {
    return ExportModelToTsDeclarations().generateTypeScriptFor(moduleKind, this)
}

private class ExportModelToTsDeclarations {
    private var declaredObjectClasses = mutableListOf<ExportedClass>()

    fun generateTypeScriptFor(name: String, moduleKind: ModuleKind, module: ExportedModule): String {
        val declareKeyword = when (moduleKind) {
            ModuleKind.PLAIN -> ""
            else -> "declare "
        }
        val types = """
           type $Nullable<T> = T | null | undefined
           ${declareKeyword}const $doNotImplementIt: unique symbol
           type $doNotImplementIt = typeof $doNotImplementIt
        """.trimIndent().prependIndent(moduleKind.indent) + "\n"

        val declarationsDts =
            types + module.declarations.toTypeScript(moduleKind)

        val namespaceName = sanitizeName(name, withHash = false)

        return when (moduleKind) {
            ModuleKind.PLAIN -> "declare namespace $namespaceName {\n$declarationsDts\n}\n"
            ModuleKind.AMD, ModuleKind.COMMON_JS, ModuleKind.ES -> declarationsDts
            ModuleKind.UMD -> "$declarationsDts\nexport as namespace $namespaceName;"
        }
    }

    fun generateTypeScriptFor(moduleKind: ModuleKind, exportedDeclarations: List<ExportedDeclaration>): String {
       return exportedDeclarations.toTypeScript(moduleKind)
    }

    private val ModuleKind.indent: String
        get() = if (this == ModuleKind.PLAIN) "    " else ""

    private fun List<ExportedDeclaration>.toTypeScript(moduleKind: ModuleKind): String {
        return joinToString("\n") {
            it.toTypeScript(
                indent = moduleKind.indent,
                prefix = if (moduleKind == ModuleKind.PLAIN) "" else "export "
            )
        }
    }

    private fun List<ExportedDeclaration>.toTypeScript(indent: String): String =
        joinToString("") { it.toTypeScript(indent) + "\n" }

    private fun ExportedDeclaration.toTypeScript(indent: String, prefix: String = ""): String =
        indent + when (this) {
            is ErrorDeclaration -> "/* ErrorDeclaration: $message */"

            is ExportedNamespace -> {
                val body = declarations.toTypeScript("$indent    ")
                val objectInstances = if (isSynthetic) "" else "${generateObjectInstancesNamespace("$indent    ")}\n"
                "${prefix}namespace $name {\n" + body + objectInstances + "$indent}"
            }

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
                        "<" + typeParameters.joinToString(", ") { it.toTypeScript(indent) } + ">"
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
                val renderedParameters = parameters.joinToString(", ") { it.toTypeScript(indent) }
                "${visibility.keyword}constructor($renderedParameters);"
            }

            is ExportedConstructSignature -> {
                val renderedParameters = parameters.joinToString(", ") { it.toTypeScript(indent) }
                "new($renderedParameters): ${returnType.toTypeScript(indent)};"
            }

            is ExportedProperty -> {
                val visibility = if (isProtected) "protected " else ""
                val keyword = when {
                    isMember -> (if (isAbstract) "abstract " else "")
                    else -> if (mutable) "let " else "const "
                }
                val possibleStatic = if (isMember && isStatic) "static " else ""
                val containsUnresolvedChar = !name.isValidES5Identifier()
                val memberName = when {
                    isMember && containsUnresolvedChar -> "\"$name\""
                    else -> name
                }
                val typeToTypeScript = type.toTypeScript(indent)
                if (isMember && !isField) {
                    val getter = "$prefix$visibility$possibleStatic${keyword}get $memberName(): $typeToTypeScript;"
                    if (!mutable) getter
                    else getter + "\n" + "$indent$prefix$visibility$possibleStatic${keyword}set $memberName(value: $typeToTypeScript);"
                } else {
                    if (!isMember && containsUnresolvedChar) ""
                    else {
                        val readonly = if (isMember && !mutable) "readonly " else ""
                        "$prefix$visibility$possibleStatic$keyword$readonly$memberName: $typeToTypeScript;"
                    }
                }
            }

            is ExportedObject ->
                generateObjectProperty().toTypeScript("", prefix).also {
                    declaredObjectClasses.add(generateObjectClass())
                }

            is ExportedClass -> {
                val keyword = if (isInterface) "interface" else "class"
                val superInterfacesKeyword = if (isInterface) "extends" else "implements"

                val superClassClause = superClass?.let { it.toExtendsClause(indent) } ?: ""
                val superInterfacesClause = superInterfaces.toImplementsClause(superInterfacesKeyword, indent)

                val members = members
                    .let { if (shouldNotBeImplemented()) it.withMagicProperty() else it }
                    .map {
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
                val klassExport =
                    "$prefix$modifiers$keyword $name$renderedTypeParameters$superClassClause$superInterfacesClause {\n$bodyString}"
                val staticsExport =
                    if (nestedClasses.isNotEmpty()) "\n" + ExportedNamespace(name, nestedClasses, true).toTypeScript(indent, prefix) else ""

                if (name.isValidES5Identifier()) klassExport + staticsExport else ""
            }
        }

    private fun ExportedObject.generateObjectClass(): ExportedClass {
        return ExportedClass(
            ir = ir,
            name = Instance,
            isInterface = false,
            isAbstract = false,
            members = members,
            superClass = superClass,
            superInterfaces = superInterfaces,
            typeParameters = emptyList(),
            nestedClasses = nestedClasses,
        )
    }

    private fun ExportedObject.generateObjectProperty(): ExportedProperty {
        val instancesNamespace = Name.identifier(objectInstances)
        val instanceRef = Name.identifier(Instance)
        val classRef = ir.getFqNameWithJsNameWhenAvailable(instancesNamespace).child(instanceRef).asString()
        val type = ExportedType.IntersectionType(
            ExportedType.ClassType(classRef, emptyList(), ir),
            ExportedType.TypeOf(classRef)
        )
        return ExportedProperty(
            name = name,
            type = type,
            mutable = false,
            isMember = ir.parent is IrClass,
            isStatic = !ir.isInner,
            isAbstract = false,
            isProtected = ir.visibility == DescriptorVisibilities.PROTECTED,
            irGetter = irGetter,
            irSetter = null,
            isField = false,
        )
    }

    private fun ExportedType.toExtendsClause(indent: String): String {
        val isImplicitlyExportedType = this is ExportedType.ImplicitlyExportedType
        val extendsClause = " extends ${toTypeScript(indent, isImplicitlyExportedType)}"
        return when {
            isImplicitlyExportedType -> " /*$extendsClause */"
            else -> extendsClause
        }
    }

    private fun List<ExportedType>.toImplementsClause(superInterfacesKeyword: String, indent: String): String {
        val (exportedInterfaces, nonExportedInterfaces) = partition { it !is ExportedType.ImplicitlyExportedType }
        val listOfNonExportedInterfaces = nonExportedInterfaces.joinToString(", ") {
            (it as ExportedType.ImplicitlyExportedType).type.toTypeScript(indent, true)
        }
        return when {
            exportedInterfaces.isEmpty() && nonExportedInterfaces.isNotEmpty() ->
                " /* $superInterfacesKeyword $listOfNonExportedInterfaces */"
            exportedInterfaces.isNotEmpty() -> {
                val nonExportedInterfacesTsString = if (nonExportedInterfaces.isNotEmpty()) "/*, $listOfNonExportedInterfaces */" else ""
                " $superInterfacesKeyword " + exportedInterfaces.joinToString(", ") { it.toTypeScript(indent) } + nonExportedInterfacesTsString
            }
            else -> ""
        }
    }

    private fun ExportedClass.shouldNotBeImplemented(): Boolean {
        return (isInterface && !ir.isExternal) || superInterfaces.any { it is ExportedType.ClassType && !it.ir.isExternal }
    }

    private fun List<ExportedDeclaration>.withMagicProperty(): List<ExportedDeclaration> {
        return plus(
            ExportedProperty(
                "__doNotUseIt",
                ExportedType.TypeParameter(doNotImplementIt),
                mutable = false,
                isMember = true,
                isStatic = false,
                isAbstract = false,
                isProtected = false,
                isField = true,
                irGetter = null,
                irSetter = null,
            )
        )
    }

    private fun IrClass.asNestedClassAccess(): String {
        val name = getJsNameOrKotlinName().identifier
        if (parent !is IrClass) return name
        return "${parentAsClass.asNestedClassAccess()}.$name"
    }

    private fun ExportedClass.withProtectedConstructors(): ExportedClass {
        return copy(members = members.map {
            if (it !is ExportedConstructor || it.isProtected) {
                it
            } else {
                it.copy(visibility = ExportedVisibility.PROTECTED)
            }
        })
    }

    private fun ExportedClass.toReadonlyProperty(): ExportedProperty {
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
            isField = false,
            irGetter = null,
            irSetter = null
        )
    }

    private fun ExportedParameter.toTypeScript(indent: String): String {
        val name = sanitizeName(name, withHash = false)
        val type = type.toTypeScript(indent)
        val questionMark = if (hasDefaultValue) "?" else ""
        return "$name$questionMark: $type"
    }

    private fun ExportedType.toTypeScript(indent: String, isInCommentContext: Boolean = false): String = when (this) {
        is ExportedType.Primitive -> typescript
        is ExportedType.Array -> "Array<${elementType.toTypeScript(indent, isInCommentContext)}>"
        is ExportedType.Function -> "(" + parameterTypes
            .withIndex()
            .joinToString(", ") { (index, type) ->
                "p$index: ${type.toTypeScript(indent, isInCommentContext)}"
            } + ") => " + returnType.toTypeScript(indent, isInCommentContext)

        is ExportedType.ClassType ->
            name + if (arguments.isNotEmpty()) "<${arguments.joinToString(", ") { it.toTypeScript(indent, isInCommentContext) }}>" else ""
        is ExportedType.TypeOf ->
            "typeof $name"

        is ExportedType.ErrorType -> if (isInCommentContext) comment else "any /*$comment*/"
        is ExportedType.Nullable -> "$Nullable<" + baseType.toTypeScript(indent, isInCommentContext) + ">"
        is ExportedType.InlineInterfaceType -> {
            members.joinToString(prefix = "{\n", postfix = "$indent}", separator = "") { it.toTypeScript("$indent    ") + "\n" }
        }
        is ExportedType.IntersectionType -> {
            lhs.toTypeScript(indent) + " & " + rhs.toTypeScript(indent, isInCommentContext)
        }
        is ExportedType.UnionType -> {
            lhs.toTypeScript(indent) + " | " + rhs.toTypeScript(indent, isInCommentContext)
        }
        is ExportedType.LiteralType.StringLiteralType -> "\"$value\""
        is ExportedType.LiteralType.NumberLiteralType -> value.toString()
        is ExportedType.ImplicitlyExportedType -> {
            val typeString = type.toTypeScript("", true)
            if (isInCommentContext) typeString else ExportedType.Primitive.Any.toTypeScript(indent) + "/* $typeString */"
        }
        is ExportedType.TypeParameter -> if (constraint == null) {
            name
        } else {
            "$name extends ${constraint.toTypeScript(indent, isInCommentContext)}"
        }
    }

    private fun generateObjectInstancesNamespace(indent: String): String {
        if (declaredObjectClasses.isEmpty()) return ""

        val result = StringBuilder()
        var previousDeclaredObjectClasses = declaredObjectClasses
        declaredObjectClasses = mutableListOf()

        while (previousDeclaredObjectClasses.isNotEmpty()) {
            result.append(previousDeclaredObjectClasses.toExportedNamespace().toTypeScript("$indent    "))
            previousDeclaredObjectClasses = declaredObjectClasses
            declaredObjectClasses = mutableListOf()
        }

        return "${indent}namespace $objectInstances {\n$result\n$indent}"
    }

    private fun List<ExportedClass>.toExportedNamespace(): List<ExportedNamespace> {
        return map {
            ExportedNamespace(it.ir.getFqNameWithJsNameWhenAvailable(false).asString(), listOf(it), true)
        }
    }
}
