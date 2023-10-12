/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.bir.generator.print

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.jetbrains.kotlin.bir.generator.Packages
import org.jetbrains.kotlin.bir.generator.model.Element
import org.jetbrains.kotlin.bir.generator.model.ListField
import org.jetbrains.kotlin.bir.generator.model.Model
import org.jetbrains.kotlin.bir.generator.model.SingleField
import org.jetbrains.kotlin.generators.tree.ImplementationKind
import org.jetbrains.kotlin.generators.tree.TypeKind
import org.jetbrains.kotlin.generators.tree.TypeRefWithNullability
import java.io.File

fun printElements(generationPath: File, model: Model) = sequence {
    for (element in model.elements) {
        if (element == model.rootElement)
            continue

        val elementName = element.toPoet()
        val selfParametrizedElementName = element.toPoetSelfParameterized()

        val elementType = when (element.kind?.typeKind) {
            null -> error("Element's category not configured")
            TypeKind.Class -> TypeSpec.classBuilder(elementName)
            TypeKind.Interface -> TypeSpec.interfaceBuilder(elementName)
        }.apply {
            addModifiers(
                when (element.kind) {
                    ImplementationKind.SealedClass -> listOf(KModifier.SEALED)
                    ImplementationKind.SealedInterface -> listOf(KModifier.SEALED)
                    ImplementationKind.AbstractClass -> listOf(KModifier.ABSTRACT)
                    ImplementationKind.FinalClass -> listOf(KModifier.FINAL)
                    ImplementationKind.OpenClass -> listOf(KModifier.OPEN)
                    else -> emptyList()
                }
            )
            addTypeVariables(element.params.map { it.toPoet() })

            val (classes, interfaces) = element.allParents.partition { it.typeKind == TypeKind.Class }
            classes.singleOrNull()?.let {
                superclass(it.toPoet())
            }
            addSuperinterfaces(interfaces.map { it.toPoet() })

            for (field in element.fields) {
                if (!field.printProperty) continue
                val poetType = field.type.toPoet().copy(nullable = field.nullable)
                addProperty(PropertySpec.builder(field.name, poetType).apply {
                    mutable(field.mutable)
                    if (field.isOverride) {
                        addModifiers(KModifier.OVERRIDE)
                    }
                    if (field.baseDefaultValue == null && field.baseGetter == null) {
                        addModifiers(KModifier.ABSTRACT)
                    }

                    field.baseDefaultValue?.let {
                        initializer(it)
                    }
                    field.baseGetter?.let {
                        getter(FunSpec.getterBuilder().addCode("return ").addCode(it).build())
                    }

                    if (field.needsDescriptorApiAnnotation) {
                        addAnnotation(descriptorApiAnnotation)
                    }

                    field.kdoc?.let {
                        addKdoc(it)
                    }

                    field.generationCallback?.invoke(this)
                }.build())
            }

            if (element.allChildren.isNotEmpty()) {
                addFunction(
                    FunSpec
                        .builder("acceptChildren")
                        .addModifiers(KModifier.OVERRIDE)
                        .addTypeVariable(TypeVariableName("D"))
                        .addParameter("visitor", elementVisitor.parameterizedBy(TypeVariableName("D")))
                        .addParameter("data", TypeVariableName("D"))
                        .apply {
                            element.allChildren.forEach { child ->
                                addCode(child.name)
                                when (child) {
                                    is SingleField -> {
                                        if (child.nullable) addCode("?")
                                        addCode(".%M(data, visitor)\n", elementAccept)
                                    }
                                    is ListField -> {
                                        addCode(".forEach { it")
                                        if ((child.elementType as? TypeRefWithNullability)?.nullable == true) addCode("?")
                                        addCode(".%M(data, visitor) }\n", elementAccept)
                                    }
                                }
                            }
                        }
                        .build()
                )
            }

            generateElementKDoc(element)

            element.generationCallback?.invoke(this)
        }.build()

        yield(printTypeCommon(generationPath, elementName.packageName, elementType, element.additionalImports))
    }
}

private fun TypeSpec.Builder.generateElementKDoc(element: Element) {
    addKdoc(buildString {
        if (element.kDoc != null) {
            appendLine(element.kDoc)
        } else {
            append("A ")
            append(if (element.isLeaf) "leaf" else "non-leaf")
            appendLine(" IR tree element.")
        }

        append("\nGenerated from: [${element.propertyName}]")
    })
}

private val descriptorApiAnnotation = ClassName("org.jetbrains.kotlin.ir", "ObsoleteDescriptorBasedAPI")
private val elementVisitor = ClassName(Packages.tree, "BirElementVisitor")
private val elementAccept = MemberName(Packages.tree, "accept", true)
