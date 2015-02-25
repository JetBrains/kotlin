// !DIAGNOSTICS: -UNUSED_PARAMETER

trait A<T> {
    fun foo(l: List<T>)
}

trait B {
    fun foo(l: List<Int>) {}
}

class <!CONFLICTING_JVM_DECLARATIONS!>C(f: A<String>)<!>: A<String> by f, B

class D(f: A<Int>): A<Int> by f, B