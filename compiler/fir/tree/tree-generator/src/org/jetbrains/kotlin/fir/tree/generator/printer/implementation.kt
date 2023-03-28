/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.fir.tree.generator.model.*
import org.jetbrains.kotlin.fir.tree.generator.model.Implementation.Kind
import org.jetbrains.kotlin.fir.tree.generator.pureAbstractElementType
import org.jetbrains.kotlin.util.SmartPrinter
import org.jetbrains.kotlin.util.withIndent
import java.io.File

fun Implementation.generateCode(generationPath: File): GeneratedFile {
    val dir = generationPath.resolve(packageName.replace(".", "/"))
    val file = File(dir, "$type.kt")
    val stringBuilder = StringBuilder()
    SmartPrinter(stringBuilder).apply {
        printCopyright()
        println("@file:Suppress(\"DuplicatedCode\")")
        println()
        println("package $packageName")
        println()
        val imports = collectImports()
        imports.forEach { println("import $it") }
        if (imports.isNotEmpty()) {
            println()
        }
        printGeneratedMessage()
        printImplementation(this@generateCode)
    }
    return GeneratedFile(file, stringBuilder.toString())
}

fun SmartPrinter.printImplementation(implementation: Implementation) {
    fun Field.transform() {
        when (this) {
            is FieldWithDefault -> origin.transform()

            is FirField ->
                println("$name = ${name}${call()}transform(transformer, data)")

            is FieldList -> {
                println("${name}.transformInplace(transformer, data)")
            }

            else -> throw IllegalStateException()
        }
    }

    with(implementation) {
        if (requiresOptIn) {
            println("@OptIn(FirImplementationDetail::class)")
        }
        if (!isPublic) {
            print("internal ")
        }
        print("${kind!!.title} $type")
        print(element.typeParameters)

        val isInterface = kind == Kind.Interface || kind == Kind.SealedInterface
        val isAbstract = kind == Kind.AbstractClass || kind == Kind.SealedClass

        fun abstract() {
            if (isAbstract) {
                print("abstract ")
            }
        }

        if (!isInterface && !isAbstract && fieldsWithoutDefault.isNotEmpty()) {
            if (isPublic) {
                print(" @FirImplementationDetail constructor")
            }
            println("(")
            withIndent {
                fieldsWithoutDefault.forEachIndexed { _, field ->
                    if (field.isParameter) {
                        println("${field.name}: ${field.typeWithArguments},")
                    } else if (!field.isFinal) {
                        printField(field, isImplementation = true, override = true, end = ",", notNull = field.notNull)
                    }
                }
            }
            print(")")
        }

        print(" : ")
        if (!isInterface && !allParents.any { it.kind == Kind.AbstractClass || it.kind == Kind.SealedClass }) {
            print("${pureAbstractElementType.type}(), ")
        }
        print(allParents.joinToString { "${it.typeWithArguments}${it.kind.braces()}" })
        println(" {")
        withIndent {
            if (isInterface || isAbstract) {
                allFields.forEach {

                    abstract()
                    printField(it, isImplementation = true, override = true, end = "", notNull = it.notNull)
                }
            } else {
                fieldsWithDefault.forEach {
                    printFieldWithDefaultInImplementation(it)
                }
                if (fieldsWithDefault.isNotEmpty()) {
                    println()
                }
            }


            val bindingCalls = element.allFields.filter {
                it.withBindThis && it.type.contains("Symbol") && it !is FieldList && it.name != "companionObjectSymbol"
            }.takeIf {
                it.isNotEmpty() && !isInterface && !isAbstract &&
                        !element.type.contains("Reference")
                        && !element.type.contains("ResolvedQualifier")
                        && !element.type.endsWith("Ref")
                        && !element.type.endsWith("AnnotationsContainer")
            }.orEmpty()

            val customCalls = fieldsWithoutDefault.filter { it.customInitializationCall != null }
            if (bindingCalls.isNotEmpty() || customCalls.isNotEmpty()) {
                println("init {")
                withIndent {
                    for (symbolField in bindingCalls) {
                        println("${symbolField.name}${symbolField.call()}bind(this)")
                    }

                    for (customCall in customCalls) {
                        customCall.optInAnnotation?.let {
                            println("@OptIn(${it.type}::class)")
                        }

                        println("${customCall.name} = ${customCall.customInitializationCall}")
                    }
                }
                println("}")
                println()
            }

            fun Field.acceptString(): String = "${name}${call()}accept(visitor, data)"
            if (!isInterface && !isAbstract) {
                print("override fun <R, D> acceptChildren(visitor: FirVisitor<R, D>, data: D) {")

                if (element.allFirFields.isNotEmpty()) {
                    println()
                    withIndent {
                        for (field in allFields.filter { it.isFirType }) {
                            if (field.withGetter || !field.needAcceptAndTransform) continue
                            when (field.name) {
                                "explicitReceiver" -> {
                                    val explicitReceiver = implementation["explicitReceiver"]!!
                                    val dispatchReceiver = implementation["dispatchReceiver"]!!
                                    val extensionReceiver = implementation["extensionReceiver"]!!
                                    println(
                                        """
                                    |${explicitReceiver.acceptString()}
                                    |        if (dispatchReceiver !== explicitReceiver) {
                                    |            ${dispatchReceiver.acceptString()}
                                    |        }
                                    |        if (extensionReceiver !== explicitReceiver && extensionReceiver !== dispatchReceiver) {
                                    |            ${extensionReceiver.acceptString()}
                                    |        }
                                        """.trimMargin(),
                                    )
                                }

                                "dispatchReceiver", "extensionReceiver", "subjectVariable", "companionObject" -> {
                                }

                                else -> {
                                    if (type == "FirWhenExpressionImpl" && field.name == "subject") {
                                        println(
                                            """
                                        |val subjectVariable_ = subjectVariable
                                        |        if (subjectVariable_ != null) {
                                        |            subjectVariable_.accept(visitor, data)
                                        |        } else {
                                        |            subject?.accept(visitor, data)
                                        |        }
                                            """.trimMargin(),
                                        )
                                    } else {
                                        when (field.origin) {
                                            is FirField -> {
                                                println(field.acceptString())
                                            }

                                            is FieldList -> {
                                                println("${field.name}.forEach { it.accept(visitor, data) }")
                                            }

                                            else -> throw IllegalStateException()
                                        }
                                    }
                                }

                            }
                        }
                    }
                }
                println("}")
                println()
            }

            abstract()
            print("override fun <D> transformChildren(transformer: FirTransformer<D>, data: D): $typeWithArguments")
            if (!isInterface && !isAbstract) {
                println(" {")
                withIndent {
                    for (field in allFields) {
                        when {
                            !field.isMutable || !field.isFirType || field.withGetter || !field.needAcceptAndTransform -> {}

                            field.name == "explicitReceiver" -> {
                                val explicitReceiver = implementation["explicitReceiver"]!!
                                val dispatchReceiver = implementation["dispatchReceiver"]!!
                                val extensionReceiver = implementation["extensionReceiver"]!!
                                if (explicitReceiver.isMutable) {
                                    println("explicitReceiver = explicitReceiver${explicitReceiver.call()}transform(transformer, data)")
                                }
                                if (dispatchReceiver.isMutable) {
                                    println(
                                        """
                                    |if (dispatchReceiver !== explicitReceiver) {
                                    |            dispatchReceiver = dispatchReceiver.transform(transformer, data)
                                    |        }
                                """.trimMargin(),
                                    )
                                }
                                if (extensionReceiver.isMutable) {
                                    println(
                                        """
                                    |if (extensionReceiver !== explicitReceiver && extensionReceiver !== dispatchReceiver) {
                                    |            extensionReceiver = extensionReceiver.transform(transformer, data)
                                    |        }
                                """.trimMargin(),
                                    )
                                }
                            }

                            field.name in setOf("dispatchReceiver", "extensionReceiver") -> {}

                            field.needsSeparateTransform -> {
                                if (!(element.needTransformOtherChildren && field.needTransformInOtherChildren)) {
                                    println("transform${field.name.replaceFirstChar(Char::uppercaseChar)}(transformer, data)")
                                }
                            }

                            !element.needTransformOtherChildren -> {
                                field.transform()
                            }

                            else -> {}
                        }
                    }
                    if (element.needTransformOtherChildren) {
                        println("transformOtherChildren(transformer, data)")
                    }
                    println("return this")
                }
                println("}")
            } else {
                println()
            }

            for (field in allFields) {
                if (!field.needsSeparateTransform) continue
                println()
                abstract()
                print("override ${field.transformFunctionDeclaration(typeWithArguments)}")
                if (isInterface || isAbstract) {
                    println()
                    continue
                }
                println(" {")
                withIndent {
                    if (field.isMutable && field.isFirType) {
                        // TODO: replace with smth normal
                        if (type == "FirWhenExpressionImpl" && field.name == "subject") {
                            println(
                                """
                                |if (subjectVariable != null) {
                                |            subjectVariable = subjectVariable?.transform(transformer, data)
                                |            subject = subjectVariable?.initializer
                                |        } else {
                                |            subject = subject?.transform(transformer, data)
                                |        }
                                    """.trimMargin(),
                            )
                        } else {
                            field.transform()
                        }
                    }
                    println("return this")
                }
                println("}")
            }

            if (element.needTransformOtherChildren) {
                println()
                abstract()
                print("override fun <D> transformOtherChildren(transformer: FirTransformer<D>, data: D): $typeWithArguments")
                if (isInterface || isAbstract) {
                    println()
                } else {
                    println(" {")
                    withIndent {
                        for (field in allFields) {
                            if (!field.isMutable || !field.isFirType || field.name == "subjectVariable") continue
                            if (!field.needsSeparateTransform) {
                                field.transform()
                            }
                            if (field.needTransformInOtherChildren) {
                                println("transform${field.name.replaceFirstChar(Char::uppercaseChar)}(transformer, data)")
                            }
                        }
                        println("return this")
                    }
                    println("}")
                }
            }

            fun generateReplace(
                field: Field,
                overridenType: Importable? = null,
                forceNullable: Boolean = false,
                body: () -> Unit,
            ) {
                println()
                if (field.name == "source") {
                    println("@FirImplementationDetail")
                }
                abstract()
                print("override ${field.replaceFunctionDeclaration(overridenType, forceNullable)}")
                if (isInterface || isAbstract) {
                    println()
                    return
                }
                print(" {")
                if (!field.isMutable) {
                    println("}")
                    return
                }
                println()
                withIndent {
                    body()
                }
                println("}")
            }

            for (field in allFields.filter { it.withReplace }) {
                val capitalizedFieldName = field.name.replaceFirstChar(Char::uppercaseChar)
                val newValue = "new$capitalizedFieldName"
                generateReplace(field, forceNullable = field.useNullableForReplace) {
                    when {
                        field.withGetter -> {}

                        field.origin is FieldList && !field.isMutableOrEmpty -> {
                            println("${field.name}.clear()")
                            println("${field.name}.addAll($newValue)")
                        }

                        else -> {
                            if (field.useNullableForReplace) {
                                println("require($newValue != null)")
                            }
                            print("${field.name} = $newValue")
                            if (field.origin is FieldList && field.isMutableOrEmpty) {
                                print(".toMutableOrEmpty()")
                            }
                            println()
                        }
                    }
                }

                for (overridenType in field.overridenTypes) {
                    generateReplace(field, overridenType) {
                        println("require($newValue is ${field.typeWithArguments})")
                        println("replace$capitalizedFieldName($newValue)")
                    }
                }
            }
        }
        println("}")
    }
}
