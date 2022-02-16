class A {
    var number: String
        internal field = 10
        get() = field.toString()
        set(newValue) {
            field = newValue.length
        }
}

fun A.test(): String {
    if (number != "10") {
        return "FAIL: expected \"10\", was ${number}"
    }

    number = "20"

    if (number != "2") {
        return "FAIL: expected \"2\", was ${number}"
    }

    number#field = 20

    if (number != "20") {
        return "FAIL: expected \"20\", was ${number}"
    }

    number = "100"
    val rawValue: Int = number#field

    if (rawValue != 3) {
        return "FAIL: expected \"3\", was $rawValue"
    }

    return "OK"
}

fun box() = A().test()
