// !CHECK_TYPE
// !EXPLICIT_FLEXIBLE_TYPES

fun foo(
        p1: ft<MutableMap<Int, String>, Map<Int, String>?>,
        p2: Map<Int, String>
) = p1 == p2

fun foo(
        p1: ft<String, String?>,
        p2: String
) = p1 == p2