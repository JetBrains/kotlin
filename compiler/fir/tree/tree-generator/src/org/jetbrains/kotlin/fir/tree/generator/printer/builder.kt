/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.fir.tree.generator.declarationAttributesType
import org.jetbrains.kotlin.fir.tree.generator.model.*
import org.jetbrains.kotlin.generators.tree.typeWithArguments
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.withIndent
import java.io.File

fun Builder.generateCode(generationPath: File): GeneratedFile {
    val dir = generationPath.resolve(packageName.replace(".", "/"))
    val file = File(dir, "$type.kt")
    val stringBuilder = StringBuilder()
    SmartPrinter(stringBuilder).apply {
        printCopyright()
        println("@file:Suppress(\"DuplicatedCode\", \"unused\")")
        println()
        println("package $packageName")
        println()
        val imports = collectImports()
        imports.forEach { println("import $it") }
        if (imports.isNotEmpty()) {
            println()
        }
        printGeneratedMessage()
        printBuilder(this@generateCode)
    }
    return GeneratedFile(file, stringBuilder.toString())
}

private fun SmartPrinter.printBuilder(builder: Builder) {
    if (builder is LeafBuilder && builder.allFields.isEmpty()) {
        printDslBuildFunction(builder, false)
        return
    }

    println("@FirBuilderDsl")
    when (builder) {
        is IntermediateBuilder -> print("interface ")
        is LeafBuilder -> {
            if (builder.isOpen) {
                print("open ")
            }
            print("class ")
        }
    }
    print(builder.typeWithArguments)
    if (builder.parents.isNotEmpty()) {
        print(builder.parents.joinToString(separator = ", ", prefix = " : ") { it.type })
    }
    var hasRequiredFields = false
    println(" {")
    withIndent {
        var needNewLine = false
        for (field in builder.allFields) {
            val (newLine, requiredFields) = printFieldInBuilder(field, builder, fieldIsUseless = false)
            needNewLine = newLine
            hasRequiredFields = hasRequiredFields || requiredFields
        }
        val hasBackingFields = builder.allFields.any { it.nullable }
        if (needNewLine) {
            println()
        }
        val buildType = when (builder) {
            is LeafBuilder -> builder.implementation.element.typeWithArguments
            is IntermediateBuilder -> builder.materializedElement!!.typeWithArguments.replace(Regex("<.>"), "<*>")
        }
        if (builder is LeafBuilder && builder.implementation.isPublic) {
            println("@OptIn(FirImplementationDetail::class)")
        }
        if (builder.parents.isNotEmpty()) {
            print("override ")
        }
        print("fun build(): $buildType")
        if (builder is LeafBuilder) {
            println(" {")
            withIndent {
                println("return ${builder.implementation.type}(")
                withIndent {
                    for (field in builder.allFields) {
                        if (field.invisibleField) continue
                        val name = field.name
                        print(name)
                        if (field.isMutableOrEmpty) print(".toMutableOrEmpty()")
                        println(",")
                    }
                }
                println(")")
            }
            println("}")
            if (hasBackingFields) {
                println()
            }
        } else {
            println()
        }

        if (builder is LeafBuilder) {
//            for (field in builder.allFields) {
//                printBackingFieldIfNeeded(field)
//            }

            val hasUselessFields = builder.uselessFields.isNotEmpty()
            if (hasUselessFields) {
                println()
                builder.uselessFields.forEachIndexed { index, field ->
                    if (index > 0) {
                        println()
                    }
                    printFieldInBuilder(field, builder, fieldIsUseless = true)
                }
            }
        }
    }
    println("}")
    if (builder is LeafBuilder) {
        println()
        printDslBuildFunction(builder, hasRequiredFields)

        if (builder.wantsCopy) {
            println()
            printDslBuildCopyFunction(builder, hasRequiredFields)
        }
    }
}

internal val Field.invisibleField: Boolean get() = customInitializationCall != null

private fun FieldWithDefault.needBackingField(fieldIsUseless: Boolean) =
    (!nullable || notNull) && origin !is FieldList && if (fieldIsUseless) {
        defaultValueInImplementation == null
    } else {
        defaultValueInBuilder == null
    }

private fun FieldWithDefault.needNotNullDelegate(fieldIsUseless: Boolean) = needBackingField(fieldIsUseless) && (type == "Boolean" || type == "Int")


private fun SmartPrinter.printFieldInBuilder(field: FieldWithDefault, builder: Builder, fieldIsUseless: Boolean): Pair<Boolean, Boolean> {
    if (field.withGetter && !fieldIsUseless || field.invisibleField) return false to false
    if (field.origin is FieldList) {
        printFieldListInBuilder(field.origin, builder, fieldIsUseless)
        return true to false
    }
    val name = field.name
    val type = field.getTypeWithArguments(field.notNull)
    val defaultValue = if (fieldIsUseless)
        field.defaultValueInImplementation.also { requireNotNull(it) }
    else
        field.defaultValueInBuilder

    printDeprecationOnUselessFieldIfNeeded(field, builder, fieldIsUseless)
    printModifiers(builder, field, fieldIsUseless)
    print("var $name: $type")
    var hasRequiredFields = false
    val needNewLine = when {
        fieldIsUseless -> {
            println()
            withIndent {
                println("get() = throw IllegalStateException()")
                println("set(_) {")
                withIndent {
                    println("throw IllegalStateException()")
                }
                println("}")
            }
            true
        }

        builder is IntermediateBuilder -> {
            println()
            false
        }
        field.needNotNullDelegate(fieldIsUseless) -> {
            println(" by kotlin.properties.Delegates.notNull<${field.type}>()")
            hasRequiredFields = true
            true
        }

        field.needBackingField(fieldIsUseless) -> {
//            println()
//            withIndent {
//                println("get() = _$name ?: throw IllegalArgumentException(\"$name should be initialized\")")
//                println("set(value) {")
//                withIndent {
//                    println("_$name = value")
//                }
//                println("}")
//                println()
//            }
//            false
            println()
            hasRequiredFields = true
            true
        }
        else -> {
            println(" = $defaultValue")
            true
        }
    }
    return needNewLine to hasRequiredFields
}

private fun SmartPrinter.printDeprecationOnUselessFieldIfNeeded(field: Field, builder: Builder, fieldIsUseless: Boolean) {
    if (fieldIsUseless) {
        println("@Deprecated(\"Modification of '${field.name}' has no impact for ${builder.type}\", level = DeprecationLevel.HIDDEN)")
    }
}

private fun SmartPrinter.printFieldListInBuilder(field: FieldList, builder: Builder, fieldIsUseless: Boolean) {
    printDeprecationOnUselessFieldIfNeeded(field, builder, fieldIsUseless)
    printModifiers(builder, field, fieldIsUseless)
    print("val ${field.name}: ${field.getMutableType(forBuilder = true)}")
    if (builder is LeafBuilder) {
        print(" = mutableListOf()")
    }
    println()
}

private fun SmartPrinter.printModifiers(builder: Builder, field: Field, fieldIsUseless: Boolean) {
    if (builder is IntermediateBuilder) {
        print("abstract ")
    }
    if (builder.isFromParent(field)) {
        print("override ")
    } else if (builder is LeafBuilder && builder.isOpen) {
        print("open ")
    }
    if (builder is LeafBuilder && field is FieldWithDefault && field.needBackingField(fieldIsUseless) && !fieldIsUseless && !field.needNotNullDelegate(fieldIsUseless)) {
        print("lateinit ")
    }
}

private fun SmartPrinter.printDslBuildFunction(
    builder: LeafBuilder,
    hasRequiredFields: Boolean
) {
    val isEmpty = builder.allFields.isEmpty()
    if (!isEmpty) {
        println("@OptIn(ExperimentalContracts::class)")
        print("inline ")
    } else if(builder.implementation.isPublic) {
        println("@OptIn(FirImplementationDetail::class)")
    }
    print("fun ")
    builder.implementation.element.typeArguments.takeIf { it.isNotEmpty() }?.let {
        print(it.joinToString(separator = ", ", prefix = "<", postfix = "> ") { it.name })
    }
    val builderType = builder.typeWithArguments
    val name = builder.implementation.name?.replaceFirst("Fir", "") ?: builder.implementation.element.name
    print("build${name}(")
    if (!isEmpty) {
        print("init: $builderType.() -> Unit")
        if (!hasRequiredFields) {
            print(" = {}")
        }
    }
    println("): ${builder.implementation.element.typeWithArguments} {")
    withIndent {
        if (!isEmpty) {
            println("contract {")
            withIndent {
                println("callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)")
            }
            println("}")
        }
        print("return ")
        if (isEmpty) {
            println("${builder.implementation.type}()")
        } else {
            println("$builderType().apply(init).build()")
        }
    }
    println("}")
}

private fun SmartPrinter.printDslBuildCopyFunction(
    builder: LeafBuilder,
    hasRequiredFields: Boolean,
) {
    val optIns =
        builder.allFields.filter { !it.invisibleField }.mapNotNullTo(mutableSetOf("ExperimentalContracts")) { it.optInAnnotation?.type }
    println("@OptIn(${optIns.joinToString { "$it::class" }})")
    print("inline ")
    print("fun ")
    builder.implementation.element.typeArguments.takeIf { it.isNotEmpty() }?.let {
        print(it.joinToString(separator = ", ", prefix = "<", postfix = "> ") { it.name })
    }
    val builderType = builder.typeWithArguments
    val name = builder.implementation.name?.replaceFirst("Fir", "") ?: builder.implementation.element.name
    print("build${name}Copy(")
    print("original: ${builder.implementation.element.typeWithArguments}, init: $builderType.() -> Unit")
    if (!hasRequiredFields) {
        print(" = {}")
    }
    println("): ${builder.implementation.element.typeWithArguments} {")
    withIndent {
        println("contract {")
        withIndent {
            println("callsInPlace(init, kotlin.contracts.InvocationKind.EXACTLY_ONCE)")
        }
        println("}")
        println("val copyBuilder = $builderType()")
        for (field in builder.allFields) {
            when {
                field.invisibleField -> {}
                field.origin is FieldList -> println("copyBuilder.${field.name}.addAll(original.${field.name})")
                field.type == declarationAttributesType.type -> println("copyBuilder.${field.name} = original.${field.name}.copy()")
                field.notNull -> println("original.${field.name}?.let { copyBuilder.${field.name} = it }")
                else -> println("copyBuilder.${field.name} = original.${field.name}")
            }
        }
        println("return copyBuilder.apply(init).build()")
    }
    println("}")
}
