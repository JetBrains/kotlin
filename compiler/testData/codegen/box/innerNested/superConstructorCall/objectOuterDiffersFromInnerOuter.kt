fun <T> eval(fn: () -> T) = fn()

class A {
    fun bar(): Any {
        return eval {
            eval {
                object : Inner() {
                    override fun toString() = foo()
                }
            }
        }
    }

    open inner class Inner
    fun foo() = "OK"
}

fun box(): String = A().bar().toString()
