// IGNORE_BACKEND: JVM
class X(val x: String) {
    open inner class Y {
        fun foo() = x
    }

    fun foo(s: String): String {
        with(X(s+x)) {
            val obj = object : Y() {}
            return obj.foo()
        }
    }
}

fun box() =
    X("K").foo("O")