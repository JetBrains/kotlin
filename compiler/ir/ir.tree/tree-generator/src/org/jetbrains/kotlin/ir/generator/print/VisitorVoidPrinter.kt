/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.print

import org.jetbrains.kotlin.generators.tree.AbstractVisitorVoidPrinter
import org.jetbrains.kotlin.generators.tree.ClassRef
import org.jetbrains.kotlin.generators.tree.PositionTypeParameterRef
import org.jetbrains.kotlin.generators.tree.imports.ImportCollecting
import org.jetbrains.kotlin.generators.tree.printer.ImportCollectingPrinter
import org.jetbrains.kotlin.ir.generator.elementVisitorType
import org.jetbrains.kotlin.ir.generator.irVisitorVoidType
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.Field

internal class VisitorVoidPrinter(
    importCollectingPrinter: ImportCollectingPrinter,
    override val visitorType: ClassRef<*>,
) : AbstractVisitorVoidPrinter<Element, Field>(importCollectingPrinter) {

    override val visitorSuperClass: ClassRef<PositionTypeParameterRef>
        get() = elementVisitorType

    override val allowTypeParametersInVisitorMethods: Boolean
        get() = false

    override val useAbstractMethodForRootElement: Boolean
        get() = false

    override val overriddenVisitMethodsAreFinal: Boolean
        get() = false

    override val ImportCollecting.classKDoc: String
        get() = deprecatedVisitorInterface(irVisitorVoidType)
}
