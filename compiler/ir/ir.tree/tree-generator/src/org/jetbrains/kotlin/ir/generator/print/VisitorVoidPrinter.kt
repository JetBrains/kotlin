/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.CompilerVersionOfApiDeprecation
import org.jetbrains.kotlin.DeprecatedCompilerApi
import org.jetbrains.kotlin.generators.tree.AbstractVisitorVoidPrinter
import org.jetbrains.kotlin.generators.tree.ClassRef
import org.jetbrains.kotlin.generators.tree.PositionTypeParameterRef
import org.jetbrains.kotlin.generators.tree.StandardTypes
import org.jetbrains.kotlin.generators.tree.imports.ImportCollecting
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter
import org.jetbrains.kotlin.generators.tree.withArgs
import org.jetbrains.kotlin.ir.generator.irVisitorType
import org.jetbrains.kotlin.ir.generator.legacyVisitorType
import org.jetbrains.kotlin.ir.generator.irVisitorVoidType
import org.jetbrains.kotlin.ir.generator.legacyVisitorVoidType
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.Field

internal open class VisitorVoidPrinter(
    importCollectingPrinter: ImportCollectingPrinter,
    override val visitorType: ClassRef<*>,
) : AbstractVisitorVoidPrinter<Element, Field>(importCollectingPrinter) {

    override val visitorSuperClass: ClassRef<PositionTypeParameterRef>
        get() = irVisitorType

    override val visitorSuperTypes: List<ClassRef<PositionTypeParameterRef>>
        get() = listOf(
            visitorSuperClass.withArgs(StandardTypes.unit, visitorDataType),
            legacyVisitorVoidType,
        )

    override val allowTypeParametersInVisitorMethods: Boolean
        get() = false

    override val useAbstractMethodForRootElement: Boolean
        get() = false

    override val overriddenVisitMethodsAreFinal: Boolean
        get() = false

    override fun shouldOverrideMethodWithNoDataParameter(element: Element): Boolean = true

    override val annotations: List<Annotation>
        get() = listOf(Suppress("DEPRECATED_COMPILER_API"))
}

internal class LegacyVisitorVoidPrinter(
    importCollectingPrinter: ImportCollectingPrinter,
    visitorType: ClassRef<*>,
) : VisitorVoidPrinter(importCollectingPrinter, visitorType) {

    override val visitorSuperClass: ClassRef<PositionTypeParameterRef>
        get() = legacyVisitorType

    override val visitorSuperTypes: List<ClassRef<PositionTypeParameterRef>>
        get() = listOf(visitorSuperClass.withArgs(StandardTypes.unit, visitorDataType))

    override val ImportCollecting.classKDoc: String
        get() = deprecatedVisitorInterface(irVisitorVoidType)

    override val annotations: List<Annotation>
        get() = listOf(DeprecatedCompilerApi(CompilerVersionOfApiDeprecation._2_1_20, replaceWith = irVisitorVoidType.simpleName))

    override fun shouldOverrideMethodWithNoDataParameter(element: Element): Boolean = false
}
