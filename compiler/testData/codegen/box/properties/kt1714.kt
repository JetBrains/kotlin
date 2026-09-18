// TARGET_BACKEND: JVM

interface A {
    val method : (() -> Unit )?
    val test : Integer
}

class AImpl : A {
    override val method : (() -> Unit )? = {
    }
    override val test : Integer = Integer(777)
}

fun test(a : A) {
    val method = a.method
    if (method != null) {
        method()
    }
}

fun box() : String {
    AImpl().test
    test(AImpl())
    return "OK"
}
