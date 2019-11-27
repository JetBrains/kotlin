// IGNORE_BACKEND_FIR: JVM_IR
class A {
    fun bar(): Any {
        return {
            {
                object : Inner() {
                    override fun toString() = foo()
                }
            }()
        }()
    }

    open inner class Inner
    fun foo() = "OK"
}

fun box(): String = A().bar().toString()
