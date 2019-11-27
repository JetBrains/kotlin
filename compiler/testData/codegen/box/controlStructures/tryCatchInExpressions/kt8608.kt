// IGNORE_BACKEND_FIR: JVM_IR
interface Callable {
    fun call(b: Boolean)
}

inline fun run(f: () -> Unit) { f() }

class A {
    fun foo(): String {
        run {
            val x = object : Callable {
                override fun call(b: Boolean) {
                    if (b) {
                        x()
                    } else {
                        try {
                            x()
                        } catch(t: Throwable) {
                        }
                    }
                }
            }
        }
        return "OK"
    }

    private fun x() {}
}

fun box(): String =
        A().foo()