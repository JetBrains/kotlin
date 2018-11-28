fun test(a: Any, b: Any, c: Any): String {
    return "a:" <caret>+ a.toString() + ", b:" + b.toString() + "_ c:" + c.toString("")
}
fun Any.toString(s: String) = ""