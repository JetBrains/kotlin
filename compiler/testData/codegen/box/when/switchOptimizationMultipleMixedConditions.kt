// WITH_STDLIB
private fun parse(text: String) = when (text) {
    Numbers.One.name, "one", "1" -> 1
    Numbers.Two.name, "two", "2" -> 2
    else -> -1
}

enum class Numbers {
    One,
    Two,
}

fun box(): String {

    val oneParsed = parse("one")
    if (oneParsed != 1) return "'one' should map to '1' but was $oneParsed"

    val OneParsed = parse("One")
    if (OneParsed != 1) return "'One' should map to '1' but was $OneParsed"

    return "OK"
}