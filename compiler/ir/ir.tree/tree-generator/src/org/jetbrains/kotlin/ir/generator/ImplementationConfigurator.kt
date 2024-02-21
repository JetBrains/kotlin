/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator

import org.jetbrains.kotlin.ir.generator.config.AbstractIrTreeImplementationConfigurator

object ImplementationConfigurator : AbstractIrTreeImplementationConfigurator() {
    override fun configure(): Unit = with(IrTree) {
        impl(simpleFunction)
        impl(property)
    }

    override fun configureAllImplementations() {
        // Use configureFieldInAllImplementations to customize certain fields in all implementation classes
    }
}