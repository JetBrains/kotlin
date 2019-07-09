// !DUMP_DEPENDENCIES

// FILE: external.kt
// EXTERNAL_FILE
class Outer {
    inner class Inner {
        fun foo() {}
    }
    fun bar() {}
}

// FILE: kotlinInnerClass.kt
// FIR_IDENTICAL
fun test(inner: Outer.Inner) = inner.foo()