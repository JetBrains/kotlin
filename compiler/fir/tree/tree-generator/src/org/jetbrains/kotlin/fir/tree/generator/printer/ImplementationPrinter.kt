/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.fir.tree.generator.printer

import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.fir.tree.generator.*
import org.jetbrains.kotlin.fir.tree.generator.model.*
import org.jetbrains.kotlin.fir.tree.generator.model.ListField
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.printer.*
import org.jetbrains.kotlin.utils.withIndent

private class ImplementationFieldPrinter(printer: ImportCollectingPrinter) : AbstractFieldPrinter<Field>(printer) {
    override fun forceMutable(field: Field): Boolean = field.isMutable && (field !is ListField || field.isMutableOrEmptyList)

    override fun actualTypeOfField(field: Field) = field.getMutableType()

    override val wrapOptInAnnotations
        get() = true
}

internal class ImplementationPrinter(
    printer: ImportCollectingPrinter
) : AbstractImplementationPrinter<Implementation, Element, Field>(printer) {

    override val implementationOptInAnnotation: ClassRef<*>
        get() = firImplementationDetailType


    override fun getPureAbstractElementType(implementation: Implementation): ClassRef<*> =
        pureAbstractElementType

    override fun makeFieldPrinter(printer: ImportCollectingPrinter): AbstractFieldPrinter<Field> = ImplementationFieldPrinter(printer)

    override fun ImportCollectingPrinter.printAdditionalMethods(implementation: Implementation) {
        fun Field.transform() {
            when (this) {
                is SimpleField ->
                    println("$name = ${name}${call()}transform(transformer, data)")

                is ListField -> {
                    addImport(transformInPlaceImport)
                    println("${name}.transformInplace(transformer, data)")
                }
            }
        }
        with(implementation) {
            val isInterface = kind == ImplementationKind.Interface || kind == ImplementationKind.SealedInterface
            val isAbstract = kind == ImplementationKind.AbstractClass || kind == ImplementationKind.SealedClass
            val bindingCalls = element.allFields.filter {
                it.withBindThis && it.hasSymbolType && it !is ListField && it.name != "companionObjectSymbol"
            }.takeIf {
                it.isNotEmpty() && !isInterface && !isAbstract &&
                        !element.typeName.contains("Reference")
                        && !element.typeName.contains("ResolvedQualifier")
                        && !element.typeName.endsWith("Ref")
            }.orEmpty()

            val customCalls = fieldsInConstructor.filter { it.customInitializationCall != null }
            if (bindingCalls.isNotEmpty() || customCalls.isNotEmpty()) {
                println()
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
            }

            fun Field.acceptString(): String = "${name}${call()}accept(visitor, data)"

            if (hasAcceptChildrenMethod) {
                printAcceptChildrenMethod(this, firVisitorType, TypeVariable("R"), override = true)
                print(" {")

                val walkableFields = walkableChildren
                if (walkableFields.isNotEmpty()) {
                    println()
                    withIndent {
                        for (field in walkableFields) {
                            when (field.name) {
                                "explicitReceiver" -> {
                                    val explicitReceiver = implementation["explicitReceiver"]
                                    val dispatchReceiver = implementation["dispatchReceiver"]
                                    val extensionReceiver = implementation["extensionReceiver"]
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
                                        when (field) {
                                            is SimpleField -> {
                                                println(field.acceptString())
                                            }

                                            is ListField -> {
                                                println(field.name, field.call(), "forEach { it.accept(visitor, data) }")
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
            }

            if (hasTransformChildrenMethod) {
                printTransformChildrenMethod(
                    implementation,
                    firTransformerType,
                    implementation,
                    modality = Modality.ABSTRACT.takeIf { isAbstract },
                    override = true,
                )
                if (!isInterface && !isAbstract) {
                    printBlock {
                        for (field in transformableChildren) {
                            when {
                                field.name == "explicitReceiver" -> {
                                    val explicitReceiver = implementation["explicitReceiver"]
                                    val dispatchReceiver = implementation["dispatchReceiver"]
                                    val extensionReceiver = implementation["extensionReceiver"]
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

                                field.withTransform -> {
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
                }
            }

            for (field in allFields) {
                if (!field.withTransform) continue
                println()
                transformFunctionDeclaration(field, implementation, override = true, kind!!)
                if (isInterface || isAbstract) {
                    println()
                    continue
                }
                printBlock {
                    if (field.isMutable && field.containsElement) {
                        // TODO: replace with smth normal
                        if (typeName == "FirWhenExpressionImpl" && field.name == "subject") {
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
            }

            if (element.needTransformOtherChildren) {
                println()
                transformOtherChildrenFunctionDeclaration(implementation, override = true, kind!!)
                if (isInterface || isAbstract) {
                    println()
                } else {
                    printBlock {
                        for (field in allFields) {
                            if (!field.isMutable || !field.containsElement || field.name == "subjectVariable") continue
                            if (!field.withTransform) {
                                field.transform()
                            }
                            if (field.needTransformInOtherChildren) {
                                println("transform${field.name.replaceFirstChar(Char::uppercaseChar)}(transformer, data)")
                            }
                        }
                        println("return this")
                    }
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
                replaceFunctionDeclaration(field, override = true, kind!!, overridenType, forceNullable)
                if (isInterface || isAbstract) {
                    println()
                    return
                }
                print(" {")
                if (!field.isMutable) {
                    if (field.name == "coneTypeOrNull") {
                        println()
                        withIndent {
                            println("require(newConeTypeOrNull == coneTypeOrNull) { \"\${javaClass.simpleName}.replaceConeTypeOrNull() called with invalid type '\${newConeTypeOrNull}'. Current type is '\$coneTypeOrNull'\" }")
                        }
                    }
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
                generateReplace(field, forceNullable = field.receiveNullableTypeInReplace) {
                    when {
                        field.implementationDefaultStrategy!!.withGetter -> {}

                        field is ListField && !field.isMutableOrEmptyList -> {
                            println("if (${field.name} === $newValue) return")
                            println("${field.name}.clear()")
                            println("${field.name}.addAll($newValue)")
                        }

                        else -> {
                            if (field.receiveNullableTypeInReplace && !field.typeRef.nullable) {
                                println("require($newValue != null)")
                            }
                            print("${field.name} = $newValue")
                            if (field is ListField && field.isMutableOrEmptyList) {
                                addImport(toMutableOrEmptyImport)
                                print(".toMutableOrEmpty()")
                            }
                            println()
                        }
                    }
                }

                val additionalOverriddenTypes =
                    field.overriddenFields.map { it.typeRef.copy(nullable = false) }.toSet() - field.typeRef.copy(nullable = false)
                for (overriddenType in additionalOverriddenTypes) {
                    generateReplace(field, overriddenType) {
                        println("require($newValue is ${field.typeRef.render()})")
                        println("replace$capitalizedFieldName($newValue)")
                    }
                }
            }
        }
    }
}

private val Field.hasSymbolType: Boolean
    get() = (typeRef as? ClassRef<*>)?.simpleName?.contains("Symbol") ?: false
