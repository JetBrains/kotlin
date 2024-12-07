/*
 * Copyright 2010-2024 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.generator

import org.jetbrains.kotlin.generators.tree.Model
import org.jetbrains.kotlin.ir.generator.config.symbol.AbstractIrSymbolTreeImplementationConfigurator
import org.jetbrains.kotlin.ir.generator.model.symbol.Symbol

object SymbolImplementationConfigurator : AbstractIrSymbolTreeImplementationConfigurator() {

    override fun configure(model: Model<Symbol>): Unit = with(IrSymbolTree) {
        impl(anonymousInitializerSymbol) {
            noSignature()
            implementation.generationCallback = {
                println()
                println("@OptIn(", obsoleteDescriptorBasedApiAnnotation.render(), "::class)")
                println("constructor(irClassSymbol: ${classSymbol.render()}) : this(irClassSymbol.descriptor)")
            }
        }

        impl(externalPackageFragmentSymbol) {
            noSignature()
        }

        impl(fileSymbol) {
            noSignature()
        }

        impl(localDelegatedPropertySymbol) {
            noSignature()
        }

        impl(returnableBlockSymbol) {
            noSignature()
        }

        impl(variableSymbol) {
            noSignature()
        }
    }

    override fun configureAllImplementations(model: Model<Symbol>) {
        configureAllImplementations {
            publicImplementation()
            implementation.putImplementationOptInInConstructor = false
        }
    }
}