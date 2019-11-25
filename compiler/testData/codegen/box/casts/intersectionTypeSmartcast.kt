// IGNORE_BACKEND_FIR: JVM_IR
interface A {
    fun foo(): Any?
}

interface B {
    fun foo(): String
}

fun bar(x: Any?): String {
    if (x is A) {
        val k = x.foo()
        if (k != "OK") return "fail 1"
    }

    if (x is B) {
        val k = x.foo()
        if (k.length != 2) return "fail 2"
    }

    if (x is A && x is B) {
        return x.foo()
    }

    return "fail 4"
}

fun box(): String = bar(object : A, B { override fun foo() = "OK" })