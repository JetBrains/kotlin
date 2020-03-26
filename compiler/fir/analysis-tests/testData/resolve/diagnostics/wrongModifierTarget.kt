class Alpha {
    inner class Beta(val int: Int = 2, double: Double = 3.0) {
        val bar = double
        operator fun component1(): Int = int
        operator fun component2(): Double = int * bar
    }

    fun foo(doubleWithoutDefaultValue: Double) {
        val (_, entry) = Beta()
    }
}

/*
val foo = 1

fun funFoo() {
    fun funOof() = 2
}

class A {
    val bar = 2

    fun funBar() = 1
}

fun B() {
    val baz = 3

    fun funDuh() {
        fun funUgh() = 1
    }
}
*/