// FIR_IDENTICAL
// DUMP_EXTERNAL_CLASS: Outer

// FILE: external.kt
// EXTERNAL_FILE
class Outer {
    inner class Inner {
        fun foo() {}
    }
    fun bar() {}
}

// FILE: kotlinInnerClass.kt
fun test(inner: Outer.Inner) = inner.foo()
