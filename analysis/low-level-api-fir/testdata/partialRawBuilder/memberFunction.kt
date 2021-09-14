/*
 * Copyright 2010-2021 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

// FUNCTION: foo

package test.classes

class Outer {
    inner class Inner {
        fun bar()

        fun foo() {
            val outer = Outer()
            val inner = outer.Inner()
            inner.bar()
        }
    }
}