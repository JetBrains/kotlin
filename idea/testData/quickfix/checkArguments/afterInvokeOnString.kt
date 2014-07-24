// "Add name to argument: 'b = 42'" "true"
fun String.invoke(a: Int, b: Int) {}

fun g() {
    ""(a = 1, b = 42)
}
