// !DIAGNOSTICS: -UNUSED_PARAMETER

open class B {
    fun foo(l: List<String>) {}
}

class C : B() {
    fun foo(l: List<Int>) {}
}