import a.*

fun main(args: Array<String>) {
    val f = ::topLevel
    val x1 = f.get()
    if (x1 != 42) throw AssertionError("Fail x1: $x1")
    f.set(239)
    val x2 = f.get()
    if (x2 != 239) throw AssertionError("Fail x2: $x2")

    val g = String::extension
    val y1 = g.get("abcde")
    if (y1 != 5L) throw AssertionError("Fail y1: $y1")
}
