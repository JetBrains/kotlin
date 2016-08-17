// IS_APPLICABLE: false

fun test(n: Int): String {
    val a: String

    <caret>if (n == 1)
        a = "one"
    else if (n == 2)
        a = "two"

    return "three"
}