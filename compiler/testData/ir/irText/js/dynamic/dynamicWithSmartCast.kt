// TARGET_BACKEND: JS_IR
// WITH_STDLIB

// NO_SIGNATURE_DUMP
// ^ KT-57566

fun test1(d: dynamic) = if (d is String) d.length else -1

fun test2(d: dynamic) = if (d is Array<*>) d.size else -1
