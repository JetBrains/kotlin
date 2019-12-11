interface A {
    fun test(): String = "A"
}

interface B : A {
    override fun test(): Unit = "B"
}

open class C : A

class D : C(), B
