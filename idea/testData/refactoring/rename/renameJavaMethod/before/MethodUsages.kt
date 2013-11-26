package testing.kotlin

import testing.JavaClass

class KotlinClass: JavaClass() {
    override fun foo(): String = "Override"
}

fun usages() {
    val a = JavaClass().foo()
    val b = KotlinClass().foo()
}