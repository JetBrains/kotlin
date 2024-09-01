// FIR_IDENTICAL

class A {
    fun <T : Function0<String>> eased(p1: T, p2: T): Int {
        val x: A = A()
        var y: Int = x.eased<Function0<String>>({ "" }, p2)
        return y
    }
}