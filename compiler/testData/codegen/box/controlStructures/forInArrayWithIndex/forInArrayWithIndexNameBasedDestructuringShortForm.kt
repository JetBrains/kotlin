// ISSUE: KT-80243
// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_PHASE: 2.2.0
// WITH_STDLIB

import kotlin.test.assertEquals

fun test1(arr: Array<String>) = buildString {
    for ((index, value) in arr.withIndex()) {
        append("$index:$value;")
    }
}

fun test2(arr: Array<String>) = buildString {
    for ((value, index) in arr.withIndex()) {
        append("$value:$index;")
    }
}

fun test3(arr: Array<String>) = buildString {
    for ((v = value, i = index) in arr.withIndex()) {
        append("$v:$i;")
    }
}

fun test4(arr: Array<String>) = buildString {
    for ((index = value, value = index) in arr.withIndex()) {
        append("$index:$value;")
    }
}

fun test5(arr: Array<String>) = buildString {
    for ((a = index, b = index, c = value) in arr.withIndex()) {
        append("$a:$b:$c;")
    }
}

fun test6(arr: Array<String>) = buildString {
    for ((a = index, b = value, c = value) in arr.withIndex()) {
        append("$a:$b:$c;")
    }
}

fun test7(arr: Array<String>) = buildString {
    for ((value) in arr.withIndex()) {
        append("$value;")
    }
}

fun box(): String {
    val arr = arrayOf("a", "b", "c")

    assertEquals("0:a;1:b;2:c;", test1(arr))
    assertEquals("a:0;b:1;c:2;", test2(arr))
    assertEquals("a:0;b:1;c:2;", test3(arr))
    assertEquals("a:0;b:1;c:2;", test4(arr))
    assertEquals("0:0:a;1:1:b;2:2:c;", test5(arr))
    assertEquals("0:a:a;1:b:b;2:c:c;", test6(arr))
    assertEquals("a;b;c;", test7(arr))

    return "OK"
}
