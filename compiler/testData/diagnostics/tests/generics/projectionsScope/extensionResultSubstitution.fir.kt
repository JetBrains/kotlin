// !DIAGNOSTICS: -UNUSED_PARAMETER

class A<T> {
    fun foo() = 1
}

interface B

public fun <E : B> E.bar() : A<out E> = null!!

fun baz(x: B) {
    x.bar().foo()
}
