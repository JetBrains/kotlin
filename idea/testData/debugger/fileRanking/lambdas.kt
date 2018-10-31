// DO_NOT_CHECK_CLASS_FQNAME
// DISABLE_STRICT_MODE

//FILE: a/a.kt
package a

fun block(l: () -> Unit) {}

class A {                        // Line with the same rank
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
// Fake Line

import a.block

class A {
    fun b() {
        val g = 5               // Line with the same rank
        val x = 1
        block { val y = 2 }
        block {
            val a = 5
            block { block { val x = 4 }}
        }
    }
}