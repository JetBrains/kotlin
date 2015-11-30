// "Add name to argument: 'b = 42'" "true"
operator fun String.invoke(a: Int, b: Int) {}

fun g() {
    ""(a = 1, <caret>42)
}
