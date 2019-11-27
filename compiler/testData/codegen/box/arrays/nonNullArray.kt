// IGNORE_BACKEND_FIR: JVM_IR
class A() {
    class B(val i: Int) {
    }

    fun test() = Array<B> (10, { B(it) })
}

fun box() = if(A().test()[5].i == 5) "OK" else "fail"
