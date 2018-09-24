// DO_NOT_CHECK_CLASS_FQNAME

//FILE: a/a.kt
package a

fun block(l: () -> Unit) {}

class A {
    fun a() {
        block {
            val a = 5
            block {
                val b = 4
            }
        }
    }
}

//FILE: b/a.kt
package b

import a.block

class A {
    fun b() {
        val g = 5
        val x = 1
        block { val y = 2 }
        block {
            val a = 5
            block { block { val x = 4 }}
        }
    }
}