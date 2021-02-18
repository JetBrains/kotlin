/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js

//class InlineFunctionReporter(private val incrementalResultsConsumer: IncrementalResultsConsumer) : IrElementVisitorVoid {
//    override fun visitElement(element: IrElement) {
//        element.acceptChildren(this, null)
//    }
//
//    override fun visitSimpleFunction(declaration: IrSimpleFunction) {
//        super.visitSimpleFunction(declaration)
//        if (declaration.isInline) {
//
//        }
//    }
//
////    private fun reportInlineFunction(function: IrSimpleFunction) {
////        val signature = function.symbol.signature ?: return
////
////        val file = function.file
////        file.fileEntry.name
////    }
//
//}