// !WITH_NEW_INFERENCE
// !DIAGNOSTICS: -UNUSED_PARAMETER

interface A<T> {
    fun foo(): Int
}

class AImpl<T>: A<T> {
    override fun foo() = 42
}

class B : A<Int> by AImpl()

fun <T> bar(): A<T> = AImpl()

class C : A<Int> by bar()

fun <T> baz(f: (T) -> T): A<T> = AImpl()

class D : A<Int> by baz({ it + 1 })

fun <T> boo(t: T): A<T> = AImpl()

class E : A<Int> by <!TYPE_MISMATCH, TYPE_MISMATCH!>boo("")<!>

class F : A<Int> by <!TYPE_MISMATCH, TYPE_MISMATCH, TYPE_MISMATCH!>AImpl<String>()<!>
