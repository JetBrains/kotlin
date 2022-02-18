class A {
    var number: String
        field = 10
        get() = field.toString()
        set(newValue) {
            field = newValue.length
        }

    fun updateNumber() {
        val oldValue: Int = number#field
        number#field = oldValue + 100
    }

    fun represent(): String {
        return "field = " + number#field
    }
}

fun box(): String {
    val a = A()

    val result = StringBuilder().apply {
        var value: String = a.number
        append("number = $value, length = " + value.length + ", ")

        a.updateNumber()

        append("number = ${a.number}, length = " + a.number.length)
    }.toString()

    return if (result == "number = 10, length = 2, number = 110, length = 3") {
        "OK"
    } else {
        "BAD: $result"
    }
}
