public fun foo(a: Any, b: <!OTHER_ERROR!>Map<!>) {
    when (a) {
        is <!OTHER_ERROR!>Map<Int><!> -> {}
        is Map -> {}
        is Map<out Any?, Any?> -> {}
        is Map<*, *> -> {}
        is Map<<!SYNTAX!><!>> -> {}
        is List<Map> -> {}
        is List -> {}
        is Int -> {}
        else -> {}
    }
}