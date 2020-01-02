public fun foo(a: Any, b: Map) {
    when (a) {
        is Map<Int> -> {}
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