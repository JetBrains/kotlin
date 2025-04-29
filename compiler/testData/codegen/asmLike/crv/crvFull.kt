// RETURN_VALUE_CHECKER_MODE: FULL
// RENDER_ANNOTATIONS
// TARGET_BACKEND: JVM_IR
// WITH_STDLIB

fun foo(): String = ""

class Outer {
    class Inner {
        fun inner() = ""
    }
    fun member() = ""
    val prop = 42
}

fun Outer.ext() = ""

val Outer.extVal: Int get() = 42

fun box() = "OK"
