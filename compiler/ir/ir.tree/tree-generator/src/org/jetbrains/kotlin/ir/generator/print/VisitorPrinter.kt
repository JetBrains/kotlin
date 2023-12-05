/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.ir.generator.*
import org.jetbrains.kotlin.ir.generator.model.*
import org.jetbrains.kotlin.ir.generator.model.Model
import org.jetbrains.kotlin.utils.SmartPrinter
import java.io.File

private open class VisitorPrinter(printer: SmartPrinter, override val visitorType: ClassRef<*>) :
    AbstractVisitorPrinter<Element, Field>(printer) {

    override val visitorTypeParameters: List<TypeVariable>
        get() = listOf(resultTypeVariable, dataTypeVariable)

    override val visitorDataType: TypeRef
        get() = dataTypeVariable

    override fun visitMethodReturnType(element: Element) = resultTypeVariable

    override val visitorSuperType: ClassRef<PositionTypeParameterRef>?
        get() = null

    override val allowTypeParametersInVisitorMethods: Boolean
        get() = false
}

fun printVisitor(generationPath: File, model: Model) = printVisitorCommon(generationPath, model, elementVisitorType, ::VisitorPrinter)

