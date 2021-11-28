fun <T> eval(fn: () -> T) = fn()

class A {
    fun bar(): Any {
        return eval {
            eval {
                class Local : Inner() {
                    override fun toString() = foo()
                }
                Local()
            }
        }
    }

    open inner class Inner
    fun foo() = "OK"
}

fun box(): String = A().bar().toString()
