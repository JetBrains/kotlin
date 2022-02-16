class A {
    var number: String
        internal field = 10
        get() = field.toString()
        set(newValue) {
            field = newValue.length
        }

    fun updateNumber() {
        number#field += 100
    }

    fun represent(): String {
        return "field = " + number#field
    }
}

fun previousNumber(a: A): Int {
    val value: Int = a.number#self#self#field#self.dec()
    return value
}
