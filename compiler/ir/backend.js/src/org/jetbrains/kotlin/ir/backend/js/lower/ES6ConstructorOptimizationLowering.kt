/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.backend.js.lower

import org.jetbrains.kotlin.ir.backend.js.JsIrBackendContext

//class ES6ConstructorOptimizationLowering(val context: JsIrBackendContext) : BodyLoweringPass {
//    override fun lower(irBody: IrBody, container: IrDeclaration) {
//        if (!context.es6mode) return
//        irBody.accept(ConstructorUsageTransformer(context, container as? IrFunction), null)
//    }
//
//    class ConstructorUsageTransformer() : IrElementTransformerVoidWithContext() {
//
//    }
//}

/**
When I don't want to optimize constructors
1. Call primary from secondary
example {
    open class A(val x: Int)

    class B: A {
        constructor(): super(4);
    }
}

2. Call secondary from primary
example {
    open class A {
        val x: Int
        constructor(x: Int) {
           this.x = x
        }
    }

3. If there is a box parameter inside the class constructor, and it extends external class
example {
    open external class Foo

    open class Bar: Foo()

    fun foo(x: Int): Any {
        return object : Bar() {
            fun foo(): Int {
                return x
            }
        }
    }
}
 */