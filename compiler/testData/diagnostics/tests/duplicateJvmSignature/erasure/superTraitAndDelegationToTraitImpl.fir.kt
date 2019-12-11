// !DIAGNOSTICS: -UNUSED_PARAMETER

interface A<T> {
    fun foo(l: List<T>)
}

interface B {
    fun foo(l: List<Int>) {}
}

class C(f: A<String>): A<String> by f, B

class D(f: A<Int>): A<Int> by f, B