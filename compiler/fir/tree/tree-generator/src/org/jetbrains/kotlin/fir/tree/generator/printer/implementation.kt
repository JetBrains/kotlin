/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.fir.tree.generator.*
import org.jetbrains.kotlin.fir.tree.generator.model.*
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.GeneratedFile
import org.jetbrains.kotlin.generators.tree.printer.braces
import org.jetbrains.kotlin.generators.tree.printer.printGeneratedType
import org.jetbrains.kotlin.generators.tree.printer.typeParameters
import org.jetbrains.kotlin.utils.SmartPrinter
import org.jetbrains.kotlin.utils.addToStdlib.ifNotEmpty
import org.jetbrains.kotlin.utils.withIndent
import java.io.File

fun Implementation.generateCode(generationPath: File): GeneratedFile =
    printGeneratedType(
        generationPath,
        TREE_GENERATOR_README,
        packageName,
        this.typeName,
        fileSuppressions = listOf("DuplicatedCode", "unused"),
    ) {
        println()
        addAllImports(usedTypes)
        printImplementation(this@generateCode)
    }

context(ImportCollector)
fun SmartPrinter.printImplementation(implementation: Implementation) {
    fun Field.transform() {
        when (this) {
            is FieldWithDefault -> origin.transform()

            is FirField ->
                println("$name = ${name}${call()}transform(transformer, data)")

            is FieldList -> {
                addImport(transformInPlaceImport)
                println("${name}.transformInplace(transformer, data)")
            }

            else -> throw IllegalStateException()
        }
    }

    with(implementation) {
        buildSet {
            if (requiresOptIn) {
                add(firImplementationDetailType)
            }

            for (field in fieldsWithoutDefault) {
                field.optInAnnotation?.let {
                    add(it)
                }
            }
        }.ifNotEmpty {
            println("@OptIn(${joinToString { "${it.render()}::class" }})")
        }

        if (!isPublic) {
            print("internal ")
        }
        print("${kind!!.title} ${this.typeName}")
        print(element.params.typeParameters(end = " "))

        val isInterface = kind == ImplementationKind.Interface || kind == ImplementationKind.SealedInterface
        val isAbstract = kind == ImplementationKind.AbstractClass || kind == ImplementationKind.SealedClass

        fun abstract() {
            if (isAbstract) {
                print("abstract ")
            }
        }

        if (!isInterface && !isAbstract && fieldsWithoutDefault.isNotEmpty()) {
            if (isPublic) {
                print(" @${firImplementationDetailType.render()} constructor")
            }
            println("(")
            withIndent {
                fieldsWithoutDefault.forEachIndexed { _, field ->
                    if (field.isParameter) {
                        print("${field.name}: ${field.typeRef.render()}")
                        if (field.nullable) print("?")
                        println(",")
                    } else if (!field.isFinal) {
                        printField(field, isImplementation = true, override = true, inConstructor = true)
                    }
                }
            }
            print(")")
        }

        print(" : ")
        if (needPureAbstractElement) {
            print("${pureAbstractElementType.render()}(), ")
        }
        print(allParents.joinToString { "${it.render()}${it.kind.braces()}" })
        println(" {")
        withIndent {
            if (isInterface || isAbstract) {
                allFields.forEach {

                    abstract()
                    printField(it, isImplementation = true, override = true)
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
                it.withBindThis && it.hasSymbolType && it !is FieldList && it.name != "companionObjectSymbol"
            }.takeIf {
                it.isNotEmpty() && !isInterface && !isAbstract &&
                        !element.typeName.contains("Reference")
                        && !element.typeName.contains("ResolvedQualifier")
                        && !element.typeName.endsWith("Ref")
                        && !element.typeName.endsWith("AnnotationsContainer")
            }.orEmpty()

            val customCalls = fieldsWithoutDefault.filter { it.customInitializationCall != null }
            if (bindingCalls.isNotEmpty() || customCalls.isNotEmpty()) {
                println("init {")
                withIndent {
                    for (symbolField in bindingCalls) {
                        println("${symbolField.name}${symbolField.call()}bind(this)")
                    }

                    for (customCall in customCalls) {
                        addAllImports(customCall.arbitraryImportables)
                        println("${customCall.name} = ${customCall.customInitializationCall}")
                    }
                }
                println("}")
                println()
            }

            fun Field.acceptString(): String = "${name}${call()}accept(visitor, data)"
            if (hasAcceptChildrenMethod) {
                print("override fun <R, D> acceptChildren(visitor: ${firVisitorType.render()}<R, D>, data: D) {")

                val walkableFields = walkableChildren
                if (walkableFields.isNotEmpty()) {
                    println()
                    withIndent {
                        for (field in walkableFields) {
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
                                    if (this.typeName == "FirWhenExpressionImpl" && field.name == "subject") {
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

            if (hasTransformChildrenMethod) {
                abstract()
                print(
                    "override fun <D> transformChildren(transformer: ",
                    firTransformerType.render(),
                    "<D>, data: D): ",
                    render(),
                )
                if (!isInterface && !isAbstract) {
                    println(" {")
                    withIndent {
                        for (field in transformableChildren) {
                            when {
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
                                    |            dispatchReceiver = dispatchReceiver?.transform(transformer, data)
                                    |        }
                                """.trimMargin(),
                                        )
                                    }
                                    if (extensionReceiver.isMutable) {
                                        println(
                                            """
                                    |if (extensionReceiver !== explicitReceiver && extensionReceiver !== dispatchReceiver) {
                                    |            extensionReceiver = extensionReceiver?.transform(transformer, data)
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
            }

            for (field in allFields) {
                if (!field.needsSeparateTransform) continue
                println()
                abstract()
                print("override ${field.transformFunctionDeclaration(this)}")
                if (isInterface || isAbstract) {
                    println()
                    continue
                }
                println(" {")
                withIndent {
                    if (field.isMutable && field.isFirType) {
                        // TODO: replace with smth normal
                        if (this.typeName == "FirWhenExpressionImpl" && field.name == "subject") {
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
                print(
                    "override fun <D> transformOtherChildren(transformer: ",
                    firTransformerType.render(),
                    "<D>, data: D): ",
                    render(),
                )
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
                overridenType: TypeRefWithNullability? = null,
                forceNullable: Boolean = false,
                body: () -> Unit,
            ) {
                println()
                if (field.name == "source") {
                    println("@${firImplementationDetailType.render()}")
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
                                addImport(toMutableOrEmptyImport)
                                print(".toMutableOrEmpty()")
                            }
                            println()
                        }
                    }
                }

                for (overridenType in field.overridenTypes) {
                    generateReplace(field, overridenType) {
                        println("require($newValue is ${field.typeRef.render()})")
                        println("replace$capitalizedFieldName($newValue)")
                    }
                }
            }
        }
        println("}")
    }
}

private val Field.hasSymbolType: Boolean
    get() = (typeRef as? ClassRef<*>)?.simpleName?.contains("Symbol") ?: false
