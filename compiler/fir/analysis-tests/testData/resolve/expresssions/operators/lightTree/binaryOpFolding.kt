// RUN_PIPELINE_TILL: BACKEND
fun cornerCases() {
    val a = "a" + "b" + "c"
    val b = "a" + ("b" + "c")
    val c = ("a" + "b") + "c"
    val d = dummy("a" + "b" + "c")
    val e = dummy("a" + "b" + "c") + "d"
    val f = "a" + dummy("b" + "c" + "d")
    val g = 1 + 2 + 3
    val h = "a\nb" + "c" + "${d}"
}

fun dummy(str: String): String {
    return str
}