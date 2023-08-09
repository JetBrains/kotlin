// FIR_IDENTICAL
// DUMP_EXTERNAL_CLASS: Outer

// KT-61141: No [primary] flag on 'DELEGATING_CONSTRUCTOR_CALL <init>'; no [operator] on kotlin.Any.equals() as "overridden:" for fake_override "equals"
// IGNORE_BACKEND: NATIVE

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
