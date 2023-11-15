/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */


package org.jetbrains.kotlin.bir.generator.print

import com.squareup.kotlinpoet.*
import org.jetbrains.kotlin.bir.generator.BirTree.rootElement
import org.jetbrains.kotlin.bir.generator.Packages
import org.jetbrains.kotlin.bir.generator.childElementList
import org.jetbrains.kotlin.bir.generator.elementBaseType
import org.jetbrains.kotlin.bir.generator.model.ListField
import org.jetbrains.kotlin.bir.generator.model.Model
import org.jetbrains.kotlin.bir.generator.model.SingleField
import org.jetbrains.kotlin.bir.generator.util.tryParameterizedBy
import org.jetbrains.kotlin.generators.tree.ImplementationKind
import org.jetbrains.kotlin.generators.tree.TypeRefWithNullability
import java.io.File

fun printElementImpls(generationPath: File, model: Model) = sequence {
    for (element in model.elements.filter { it.isLeaf }) {
        val elementType = TypeSpec.classBuilder(element.elementImplName).apply {
            addTypeVariables(element.params.map { it.toPoet() })

            if (element.kind == ImplementationKind.Interface || element.kind == ImplementationKind.SealedInterface) {
                superclass(elementBaseType.toPoet())
                addSuperinterface(element.toPoetSelfParameterized())
            } else {
                superclass(element.toPoetSelfParameterized())
            }

            if (element.ownerSymbolType != null) {
                addProperty(
                    PropertySpec
                        .builder("owner", element.elementImplName, KModifier.OVERRIDE)
                        .getter(FunSpec.getterBuilder().addCode("return this\n").build())
                        .build()
                )
            }

            val ctor = FunSpec.constructorBuilder()

            val fieldImpls = element.fieldImpls
            val allChildren = fieldImpls.filter { it.field.isChild }
            val childrenLists = allChildren.filter { it.field is ListField }

            fieldImpls.forEach { fieldImpl ->
                val field = fieldImpl.field
                val poetType = if (field is ListField && field.isChild)
                    childElementListImpl.tryParameterizedBy(field.elementType.toPoet())
                else
                    field.typeRef.toPoet().copy(nullable = field.nullable)

                if (field.passViaConstructorParameter) {
                    ctor.addParameter(field.name, poetType)
                }

                addProperty(PropertySpec.builder(field.name, poetType).apply {
                    mutable(field.isMutable)
                    addModifiers(KModifier.OVERRIDE)

                    if (field.needsDescriptorApiAnnotation) {
                        addAnnotation(
                            AnnotationSpec
                                .builder(descriptorApiAnnotation)
                                .useSiteTarget(AnnotationSpec.UseSiteTarget.PROPERTY)
                                .build()
                        )
                    }

                    if (field is ListField && field.isChild && !field.passViaConstructorParameter) {
                        initializer(
                            "%T(this, %L, %L)",
                            childElementListImpl,
                            childrenLists.indexOf(fieldImpl) + 1,
                            (field.elementType as TypeRefWithNullability).nullable
                        )
                    } else if (field.isReadWriteTrackedProperty) {
                        addProperty(
                            PropertySpec.builder(field.backingFieldName, if (field.isChild) poetType.copy(nullable = true) else poetType)
                                .mutable(true)
                                .addModifiers(KModifier.PRIVATE)
                                .apply {
                                    if (field.initializeToThis) initializer("this") else initializer("%N", field.name)
                                }
                                .build()
                        )
                        getter(
                            FunSpec.getterBuilder()
                                .addCode("return ${field.backingFieldName}")
                                .build()
                        )
                    } else {
                        if (field.initializeToThis) initializer("this") else initializer("%N", field.name)
                    }

                    if (field.isReadWriteTrackedProperty) {
                        getter(
                            FunSpec.getterBuilder()
                                .apply {
                                    addCode("recordPropertyRead(%L)\n", fieldImpl.propertyId)
                                    addCode("return ${field.backingFieldName}")
                                    if (field.isChild && !field.nullable) {
                                        addCode(" ?: throwChildElementRemoved(%S)", field.name)
                                    }
                                }.build()
                        )
                        setter(
                            FunSpec.setterBuilder()
                                .addParameter(ParameterSpec("value", poetType))
                                .apply {
                                    addCode("if (${field.backingFieldName} != value) {\n")
                                    if (field.isChild) {
                                        addCode("    childReplaced(${field.backingFieldName}, value)\n")
                                    }
                                    addCode("    ${field.backingFieldName} = value\n")
                                    addCode("    invalidate(%L)\n", fieldImpl.propertyId)
                                    addCode("}\n")
                                }.build()
                        )
                    }
                }.build())
            }

            if (fieldImpls.any { it.field.needsDescriptorApiAnnotation }) {
                ctor.addAnnotation(descriptorApiAnnotation)
            }

            allChildren.forEach { child ->
                if (child.field is SingleField) {
                    ctor.addCode("initChild(${child.field.backingFieldName})\n")
                }
            }

            primaryConstructor(ctor.build())

            if (allChildren.isNotEmpty()) {
                addFunction(
                    FunSpec
                        .builder("acceptChildrenLite")
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("visitor", elementVisitorLite)
                        .apply {
                            element.allChildren.forEach { child ->
                                when (child) {
                                    is SingleField -> {
                                        addCode(child.backingFieldName)
                                        addCode("?")
                                        addCode(".%M(visitor)\n", elementAcceptLite)
                                    }
                                    is ListField -> {
                                        addCode(child.name)
                                        addCode(".acceptChildrenLite(visitor)\n")
                                    }
                                }
                            }
                        }
                        .build()
                )

                addFunction(
                    FunSpec
                        .builder("replaceChildProperty")
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("old", rootElement.toPoet())
                        .addParameter("new", rootElement.toPoet().copy(nullable = true))
                        .returns(INT)
                        .apply {
                            addCode("return when {\n")
                            allChildren.forEach { fieldImpl ->
                                val field = fieldImpl.field
                                if (field is SingleField) {
                                    addCode("    this.%N === old -> {\n", field.backingFieldName)
                                    addCode(
                                        "        this.%N = new as %T\n",
                                        field.backingFieldName, field.typeRef.toPoet().copy(nullable = true)
                                    )
                                    addCode("        %L\n", fieldImpl.propertyId)
                                    addCode("    }\n")
                                }
                            }
                            addCode("    else -> throwChildForReplacementNotFound(old)\n")
                            addCode("}\n")
                        }
                        .build()
                )
            }

            if (childrenLists.isNotEmpty()) {
                addFunction(
                    FunSpec
                        .builder("getChildrenListById")
                        .addModifiers(KModifier.OVERRIDE)
                        .addParameter("id", INT)
                        .returns(childElementList.toPoet().tryParameterizedBy(STAR))
                        .apply {
                            addCode("return when(id) {\n")
                            childrenLists.forEachIndexed { index, fieldImpl ->
                                addCode("    %L -> this.%N\n", index + 1, fieldImpl.field.name)
                            }
                            addCode("    else -> throwChildrenListWithIdNotFound(id)\n")
                            addCode("}\n")
                        }
                        .build()
                )
            }
        }.build()

        yield(printTypeCommon(generationPath, element.elementImplName.packageName, elementType))
    }
}

private val descriptorApiAnnotation = ClassName("org.jetbrains.kotlin.ir", "ObsoleteDescriptorBasedAPI")
private val childElementListImpl = ClassName(Packages.tree, "BirImplChildElementList")
val elementVisitorLite = ClassName(Packages.tree, "BirElementVisitorLite")
val elementAcceptLite = MemberName(Packages.tree, "acceptLite", true)
