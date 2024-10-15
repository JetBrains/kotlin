// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
package f

import f.A.Companion.B

class A {
    companion object {
        class B
    }
}

fun test() = B()