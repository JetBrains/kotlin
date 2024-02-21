/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.model

import org.jetbrains.kotlin.generators.tree.AbstractImplementation
import org.jetbrains.kotlin.generators.tree.ImplementationKind
import org.jetbrains.kotlin.generators.tree.ImportCollector
import org.jetbrains.kotlin.utils.SmartPrinter

class Implementation(element: Element, name: String?) : AbstractImplementation<Implementation, Element, Field>(element, name) {
    override val allFields: List<Field> = element.allFields.map { it.copy() }

    override var kind: ImplementationKind? = ImplementationKind.FinalClass

    var generationCallback: (context(ImportCollector) SmartPrinter.() -> Unit)? = null

    override var doPrint = true

    init {
        isPublic = true
    }
}
