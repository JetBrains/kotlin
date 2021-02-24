interface I {
    val <T> T.id: T
        get() = this
}

class A(i: I) : I by i

fun box(): String = with(A(object : I {})) { "OK".id }
