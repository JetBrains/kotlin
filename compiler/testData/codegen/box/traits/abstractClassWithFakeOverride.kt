interface A {
    fun foo(): String = "OK"
}

abstract class B : A {
    abstract override fun foo(): String
}

abstract class C : B()

fun box() = "OK"