// ISSUE: KT-44942

abstract class A {
    abstract fun foo(): String
}

class B : A() {
    override fun foo(): String = "fail"

    fun bar() = "fail"
}

class C : A() {
    override fun foo(): String = "OK"
}

fun A.test() = (this as? B)?.bar() ?: foo()

fun box() = C().test()
