// IS_APPLICABLE: false
// WITH_RUNTIME
fun main(args: Array<String>) {
    val t: String? = "abc"
    if (t != null<caret>) t else throw KotlinNullPointerException()
}
