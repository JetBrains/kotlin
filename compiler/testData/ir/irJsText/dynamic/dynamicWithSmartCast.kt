// WITH_RUNTIME

fun test1(d: dynamic) = if (d is String) d.length else -1

fun test2(d: dynamic) = if (d is Array<*>) d.size else -1