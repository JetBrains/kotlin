package a.b

class C {
    object D {
        fun foo() {}
    }


    companion object {
        fun foo() {}
    }

    fun foo() {}
}

enum class E {
    entry
}

fun foo() {}

val f = 10

fun main() {
    a.b.foo()
    a.b.C.foo()
    a.b.C.D.foo()
    val x = a.b.f
    C.foo()
    C().foo()
    val e = a.b.E.entry
    val e1 = E.entry
}