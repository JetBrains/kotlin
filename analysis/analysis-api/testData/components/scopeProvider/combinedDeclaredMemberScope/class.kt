package test

abstract class A {
    fun perform() { }

    val x: Int = 0

    class C1

    object O1

    companion object {
        val y: Int = 0
    }
}

class C : A() {
    fun foo(): Int = 5

    val bar: String = ""

    class C2

    object O2

    companion object {
        val baz: String = ""
    }
}

// class: test/C
