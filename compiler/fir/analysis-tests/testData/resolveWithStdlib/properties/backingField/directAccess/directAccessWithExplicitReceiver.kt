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

    if (a.number != "10") {
        return "FAIL: expected \"10\", was ${a.number}"
    }

    a.number = "20"

    if (a.number != "2") {
        return "FAIL: expected \"2\", was ${a.number}"
    }

    a.number#field = 20

    if (a.number != "20") {
        return "FAIL: expected \"20\", was ${a.number}"
    }

    a.number = "100"
    val rawValue: Int = a.number#field

    if (rawValue != 3) {
        return "FAIL: expected \"3\", was $rawValue"
    }

    return "OK"
}
