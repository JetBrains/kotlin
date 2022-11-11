class D {
    fun stableF(): C = C()
    fun fooF(): String = stableF().o()

    val stableP1: C get() = C()
    val fooP1: String get() = stableP1.op

    val stableP2: C = C()
    val fooP2: String = stableP1.op

    fun expF(): E = TODO()
    fun barF(): String = expF().e()

    val expP1: E get() = TODO()
    val barP1: String get() = expP1.ep
}

class D2 {
    val expP2: E = TODO()
    val barP2: String = expP2.ep
}

fun bar() {
    fun foo(): E = TODO()
    foo()
}

fun baz() {
    fun qux() {
        fun foo(): E = TODO()
        foo()
    }
    qux()
}

fun quux() {
    class Local {
        fun corge() {
            fun foo(): E = TODO()
            foo()
        }
    }
    Local().corge()
}

fun grault() {
    object {
        fun garply() {
            fun foo(): E = TODO()
            foo()
        }
    }.garply()
}

fun waldo() {
    val fred = object {
        fun garply() {
            fun foo(): E = TODO()
            foo()
        }
    }
    fred.garply()
}
