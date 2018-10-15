fun test(x: Any): Int? {
    <caret>x as? String ?: return null
    return foo(x)
}

fun foo(x: String) = 1