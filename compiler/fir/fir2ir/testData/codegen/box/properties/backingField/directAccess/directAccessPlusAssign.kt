var number: String
    internal field = 10
    get() = field.toString()
    set(newValue) {
        field = newValue.length
    }

fun box(): String {
    number += "test"

    if (number != "6") {
        return "FAIL: expected 6, was $number"
    }

    number#field += 4

    if (number != "10") {
        return "FAIL: expected 10, was $number"
    }

    return "OK"
}
