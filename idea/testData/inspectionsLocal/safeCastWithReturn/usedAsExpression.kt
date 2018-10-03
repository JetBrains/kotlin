// PROBLEM: none
fun test(x: Any): Int? {
    val s = <caret>x as? String ?: return null
    return foo(s)
}

fun foo(x: String) = 1