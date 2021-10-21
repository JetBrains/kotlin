/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import com.squareup.kotlinpoet.*
import org.jetbrains.kotlin.ir.generator.BASE_PACKAGE
import org.jetbrains.kotlin.ir.generator.elementTransformerType
import org.jetbrains.kotlin.ir.generator.elementVisitorType
import org.jetbrains.kotlin.ir.generator.model.*
import org.jetbrains.kotlin.ir.generator.util.TypeKind
import org.jetbrains.kotlin.ir.generator.util.tryParameterizedBy
import java.io.File

fun printElements(generationPath: File, model: Model) = sequence {
    for (element in model.elements) {
        if (element.suppressPrint) continue

        val elementName = element.toPoet()
        val selfParametrizedElementName = element.toPoetSelfParameterized()

        val elementType = when (element.kind?.typeKind) {
            null -> error("Element's category not configured")
            TypeKind.Class -> TypeSpec.classBuilder(elementName)
            TypeKind.Interface -> TypeSpec.interfaceBuilder(elementName)
        }.apply {
            addModifiers(
                when (element.kind) {
                    Element.Kind.SealedClass -> listOf(KModifier.SEALED)
                    Element.Kind.SealedInterface -> listOf(KModifier.SEALED)
                    Element.Kind.AbstractClass -> listOf(KModifier.ABSTRACT)
                    Element.Kind.FinalClass -> listOf(KModifier.FINAL)
                    Element.Kind.OpenClass -> listOf(KModifier.OPEN)
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

                    field.generationCallback?.invoke(this)
                }.build())
            }

            val isRootElement = element.elementParents.isEmpty()
            if (element.accept) {
                addFunction(FunSpec.builder("accept").apply {
                    addModifiers(if (isRootElement) KModifier.ABSTRACT else KModifier.OVERRIDE)
                    val r = TypeVariableName("R")
                    val d = TypeVariableName("D")
                    addTypeVariable(r)
                    addTypeVariable(d)
                    addParameter("visitor", elementVisitorType.toPoet().tryParameterizedBy(r, d))
                    addParameter("data", d)
                    returns(r)
                    if (!isRootElement) {
                        addStatement("return visitor.${element.visitFunName}(this, data)")
                    }
                }.build())
            }

            if (element.transform) {
                addFunction(FunSpec.builder("transform").apply {
                    addModifiers(if (isRootElement) KModifier.ABSTRACT else KModifier.OVERRIDE)
                    val d = TypeVariableName("D")
                    addTypeVariable(d)
                    addParameter("transformer", elementTransformerType.toPoet().tryParameterizedBy(d))
                    addParameter("data", d)
                    returns(selfParametrizedElementName)
                    if (!isRootElement) {
                        addStatement("return accept(transformer, data) as %T", selfParametrizedElementName)
                    }
                }.build())
            }

            if (element.ownsChildren && (isRootElement || element.walkableChildren.isNotEmpty())) {
                addFunction(FunSpec.builder("acceptChildren").apply {
                    addModifiers(if (isRootElement) KModifier.ABSTRACT else KModifier.OVERRIDE)
                    val d = TypeVariableName("D")
                    addTypeVariable(d)
                    addParameter("visitor", elementVisitorType.toPoet().tryParameterizedBy(UNIT, d))
                    addParameter("data", d)

                    for (child in element.walkableChildren) {
                        addStatement(buildString {
                            append(child.name)
                            if (child.nullable) append("?")
                            when (child) {
                                is SingleField -> append(".accept(visitor, data)")
                                is ListField -> append(".forEach { it.accept(visitor, data) }")
                            }
                        })
                    }
                }.build())
            }

            if (element.ownsChildren && (isRootElement || element.transformableChildren.isNotEmpty())) {
                addFunction(FunSpec.builder("transformChildren").apply {
                    addModifiers(if (isRootElement) KModifier.ABSTRACT else KModifier.OVERRIDE)
                    val d = TypeVariableName("D")
                    addTypeVariable(d)
                    addParameter("transformer", elementTransformerType.toPoet().tryParameterizedBy(d))
                    addParameter("data", d)

                    for (child in element.transformableChildren) {
                        val args = mutableListOf<Any>()
                        val code = buildString {
                            when (child) {
                                is SingleField -> {
                                    append(child.name)
                                    append(" = ")
                                    append(child.name)
                                    if (child.nullable) append("?")
                                    append(".transform(transformer, data)")
                                }
                                is ListField -> {
                                    append(child.name)
                                    if (child.mutable) {
                                        append(" = ")
                                        append(child.name)
                                        if (child.nullable) append("?")
                                    }
                                    append(".%M(transformer, data)")
                                    args.add(if (child.mutable) transformIfNeeded else transformInPlace)
                                }
                            }

                            if (child is SingleField) {
                                val elRef = child.type as ElementRef
                                if (!elRef.element.transform) {
                                    append(" as")
                                    if (child.nullable) append("?")
                                    append(" %T")
                                    args.add(elRef.toPoet())
                                }
                            }
                        }

                        addStatement(code, *args.toTypedArray())
                    }
                }.build())
            }

            generateElementKDoc(element)

            element.generationCallback?.invoke(this)
        }.build()

        yield(printTypeCommon(generationPath, elementName.packageName, elementType))
    }
}

private fun TypeSpec.Builder.generateElementKDoc(element: Element) {
    addKdoc(buildString {
        append("A ")
        append(if (element.isLeaf) "leaf" else "non-leaf")
        appendLine(" IR tree element.")

        append("@sample ${element.propertyName}")
    })
}

private val descriptorApiAnnotation = ClassName("org.jetbrains.kotlin.ir", "ObsoleteDescriptorBasedAPI")
private val transformIfNeeded = MemberName("$BASE_PACKAGE.util", "transformIfNeeded", true)
private val transformInPlace = MemberName("$BASE_PACKAGE.util", "transformInPlace", true)
