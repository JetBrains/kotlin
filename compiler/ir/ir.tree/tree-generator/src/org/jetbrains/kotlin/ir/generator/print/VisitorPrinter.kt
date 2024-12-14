/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.CompilerVersionOfApiDeprecation
import org.jetbrains.kotlin.DeprecatedCompilerApi
import org.jetbrains.kotlin.generators.tree.*
import org.jetbrains.kotlin.generators.tree.imports.ImportCollecting
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter
import org.jetbrains.kotlin.ir.generator.irVisitorType
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.Field

internal class VisitorPrinter(
    importCollectingPrinter: ImportCollectingPrinter,
    override val visitorType: ClassRef<*>,
) : AbstractVisitorPrinter<Element, Field>(importCollectingPrinter) {

    override val visitorTypeParameters: List<TypeVariable>
        get() = listOf(resultTypeVariable, dataTypeVariable)

    override val visitorDataType: TypeRef
        get() = dataTypeVariable

    override fun visitMethodReturnType(element: Element) = resultTypeVariable

    override val visitorSuperTypes: List<ClassRef<PositionTypeParameterRef>>
        get() = emptyList()

    override val allowTypeParametersInVisitorMethods: Boolean
        get() = false

    override val ImportCollecting.classKDoc: String
        get() = deprecatedVisitorInterface(irVisitorType)

    override val annotations: List<Annotation>
        get() = listOf(DeprecatedCompilerApi(CompilerVersionOfApiDeprecation._2_1_20, replaceWith = "IrVisitor"))
}
