// FIR_IDENTICAL

class A {
    operator fun component1() = "O"
    operator fun component2() = "K"
}

class Foo {
    val bar =
        if (true) ""
        else {
            val (o, k) = A()
            o + k
        }

    init {
        val (o, k) = A()
        val r = o + k
    }
}