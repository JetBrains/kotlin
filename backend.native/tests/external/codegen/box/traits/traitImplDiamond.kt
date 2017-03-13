// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

interface A {
    fun foo() = "Fail"
}

interface B : A

interface C : A {
    override fun foo() = "OK"
}

interface D : B, C

class Impl : D

fun box(): String = Impl().foo()
