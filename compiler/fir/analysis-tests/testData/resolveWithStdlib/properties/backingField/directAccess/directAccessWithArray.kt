val number: String
    internal field = mutableListOf(10, 20)
    get() = field.joinToString(", ")

fun box(): String {
    number#field[1] += 4

    if (number != "10, 24") {
        return "FAIL: expected `10, 24`, was $number"
    }

    return "OK"
}
