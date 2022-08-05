class D {
    fun stableF(): C = C()
    fun fooF(): String = stableF().o()

    val stableP1: C get() = C()
    val fooP1: String get() = stableP1.op

    val stableP2: C = C()
    val fooP2: String = stableP1.op

    fun expF(): E = E()
    fun barF(): String = expF().e()

    val expP1: E get() = E()
    val barP1: String get() = expP1.ep
}

class D2 {
    val expP2: E = E()
    val barP2: String = expP2.ep
}

fun bar() {
    fun foo(): E = E()
    foo()
}

fun baz() {
    fun qux() {
        fun foo(): E = E()
        foo()
    }
    qux()
}

fun quux() {
    class Local {
        fun corge() {
            fun foo(): E = E()
            foo()
        }
    }
    Local().corge()
}

fun grault() {
    object {
        fun garply() {
            fun foo(): E = E()
            foo()
        }
    }.garply()
}

fun waldo() {
    val fred = object {
        fun garply() {
            fun foo(): E = E()
            foo()
        }
    }
    fred.garply()
}
