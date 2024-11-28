// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL

class A {
    fun <T : Function0<String>> eased(p1: T, p2: T): Int {
        val x: A = A()
        var y: Int = x.eased<Function0<String>>({ "" }, p2)
        return y
    }

    fun <T> eased2(p1: T, p2: T): Int where T : CharSequence, T : Function0<String> {
        val x: A = A()
        var y: Int = x.eased<Function0<String>>({ "" }, p2)
        return y
    }
}