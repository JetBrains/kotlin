/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import com.squareup.kotlinpoet.*
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import org.jetbrains.kotlin.ir.generator.VISITOR_PACKAGE
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.Model
import org.jetbrains.kotlin.ir.generator.util.GeneratedFile
import java.io.File

private val visitorTypeName = ClassName(VISITOR_PACKAGE, "IrElementVisitor")
private val visitorVoidTypeName = ClassName(VISITOR_PACKAGE, "IrElementVisitorVoid")
private val transformerTypeName = ClassName(VISITOR_PACKAGE, "IrElementTransformer")

fun printVisitor(generationPath: File, model: Model): GeneratedFile {
    val visitorType = TypeSpec.interfaceBuilder(visitorTypeName).apply {
        val r = TypeVariableName("R", KModifier.OUT)
        val d = TypeVariableName("D", KModifier.IN)
        addTypeVariable(r)
        addTypeVariable(d)

        fun buildVisitFun(element: Element) = FunSpec.builder(element.visitFunName).apply {
            addParameter(element.visitorParam, element.toPoetStarParameterized())
            addParameter("data", d)
            returns(r)
        }

        addFunction(buildVisitFun(model.rootElement).addModifiers(KModifier.ABSTRACT).build())

        for (element in model.elements) {
            element.visitorParent?.let { parent ->
                addFunction(buildVisitFun(element).apply {
                    addStatement("return ${parent.element.visitFunName}(${element.visitorParam}, data)")
                }.build())
            }
        }
    }.build()

    return printTypeCommon(generationPath, visitorTypeName.packageName, visitorType)
}

fun printVisitorVoid(generationPath: File, model: Model): GeneratedFile {
    val dataType = NOTHING.copy(nullable = true)

    val visitorType = TypeSpec.interfaceBuilder(visitorVoidTypeName).apply {
        addSuperinterface(visitorTypeName.parameterizedBy(UNIT, dataType))

        fun buildVisitFun(element: Element) = FunSpec.builder(element.visitFunName).apply {
            addModifiers(KModifier.OVERRIDE)
            addParameter(element.visitorParam, element.toPoetStarParameterized())
            addParameter("data", dataType)
            addStatement("return ${element.visitFunName}(${element.visitorParam})")
        }

        fun buildVisitVoidFun(element: Element) = FunSpec.builder(element.visitFunName).apply {
            addParameter(element.visitorParam, element.toPoetStarParameterized())
        }

        addFunction(buildVisitFun(model.rootElement).build())
        addFunction(buildVisitVoidFun(model.rootElement).build())

        for (element in model.elements) {
            element.visitorParent?.let { parent ->
                addFunction(buildVisitFun(element).build())
                addFunction(buildVisitVoidFun(element).apply {
                    addStatement("return ${parent.element.visitFunName}(${element.visitorParam})")
                }.build())
            }
        }
    }.build()

    return printTypeCommon(generationPath, visitorVoidTypeName.packageName, visitorType)
}

fun printTransformer(generationPath: File, model: Model): GeneratedFile {
    val visitorType = TypeSpec.interfaceBuilder(transformerTypeName).apply {
        val d = TypeVariableName("D", KModifier.IN)
        addTypeVariable(d)

        addSuperinterface(visitorTypeName.parameterizedBy(model.rootElement.toPoetStarParameterized(), d))

        fun buildVisitFun(element: Element) = FunSpec.builder(element.visitFunName).apply {
            addModifiers(KModifier.OVERRIDE)
            addParameter(element.visitorParam, element.toPoetStarParameterized())
            addParameter("data", d)
        }

        for (element in model.elements) {
            if (element.transformByChildren) {
                addFunction(buildVisitFun(element).apply {
                    addStatement("${element.visitorParam}.transformChildren(this, data)")
                    addStatement("return ${element.visitorParam}")
                    returns((element.transformerReturnType ?: element).toPoetStarParameterized())
                }.build())
            } else {
                element.visitorParent?.let { parent ->
                    addFunction(buildVisitFun(element).apply {
                        addStatement("return ${parent.element.visitFunName}(${element.visitorParam}, data)")
                        element.transformerReturnType?.let {
                            returns(it.toPoetStarParameterized())
                        }
                    }.build())
                }
            }
        }
    }.build()

    return printTypeCommon(generationPath, transformerTypeName.packageName, visitorType)
}
