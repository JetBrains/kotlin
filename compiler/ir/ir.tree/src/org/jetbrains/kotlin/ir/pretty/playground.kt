/*
 * Copyright 2010-2023 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package org.jetbrains.kotlin.ir.pretty

import org.jetbrains.kotlin.ir.expressions.IrSyntheticBodyKind

val a = buildIrFile("a.kt") {
    packageName("com.example")
    irClass("Aa") {
        debugInfo(39, 51)
        symbol("com.example.Aa")
        modalityAbstract()
        visibilityInternal()
        irConstructor {
            primary()
            irBlockBody {
                irComposite {
                    irSimpleFunction("local") {

                    }
                    irFunctionReference {

                    }
                }
                irBlock {

                }
                irReturnableBlock {

                }
                irSetValue {

                }
            }
        }
    }
    irSimpleFunction("foo") {
        symbol("com.example.foo")
        irBlockBody {
            irReturn {
                from("com.example.foo")
                irConstructorCall {

                }
            }
        }
    }
    irScript("MyScript") {
        irSimpleFunction("") {

        }
    }
}
