class A {
    var number: String
        internal field = 10
        get() = field.toString()
        set(newValue) {
            field = newValue.length
        }
}

fun box(): String {
    val a = A()
    val previousNumber: Int = a.number#self#self#field#self.dec()

    if (previousNumber != 9) {
        return "FAIL: expected \"9\", was ${previousNumber}"
    }

    return "OK"
}
