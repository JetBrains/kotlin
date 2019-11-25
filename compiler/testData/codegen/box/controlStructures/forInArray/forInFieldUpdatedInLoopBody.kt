// IGNORE_BACKEND_FIR: JVM_IR
var xs: IntArray = intArrayOf(1, 2, 3)
    get() = field
    set(ys) {
        var sum = 0
        for (x in field) {
            sum = sum * 10 + x
            field = intArrayOf(4, 5, 6)
        }
        if (sum != 123) throw AssertionError("sum=$sum")
        field = ys
    }

fun box(): String {
    xs = intArrayOf()
    return "OK"
}