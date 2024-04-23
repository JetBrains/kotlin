/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator.config

import org.jetbrains.kotlin.generators.tree.config.AbstractImplementationConfigurator
import org.jetbrains.kotlin.generators.tree.imports.ArbitraryImportable
import org.jetbrains.kotlin.ir.generator.Packages
import org.jetbrains.kotlin.ir.generator.model.Element
import org.jetbrains.kotlin.ir.generator.model.Field
import org.jetbrains.kotlin.ir.generator.model.Implementation

abstract class AbstractIrTreeImplementationConfigurator : AbstractImplementationConfigurator<Implementation, Element, Field, Field>() {
    override fun createImplementation(element: Element, name: String?) = Implementation(element, name)

    protected fun ImplementationContext.undefinedOffset(): String =
        "UNDEFINED_OFFSET".also {
            additionalImports(ArbitraryImportable(Packages.tree, it))
        }

    protected fun ImplementationContext.smartList(): String =
        "SmartList()".also {
            additionalImports(ArbitraryImportable("org.jetbrains.kotlin.utils", "SmartList"))
        }
}