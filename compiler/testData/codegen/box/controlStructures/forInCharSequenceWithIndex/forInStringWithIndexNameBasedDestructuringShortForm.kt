// ISSUE: KT-80243
// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm
// WITH_STDLIB

import kotlin.test.assertEquals

fun test1(str: String) = buildString {
    for ((index, value) in str.withIndex()) {
        append("$index:$value;")
    }
}

fun test2(str: String) = buildString {
    for ((value, index) in str.withIndex()) {
        append("$value:$index;")
    }
}

fun test3(str: String) = buildString {
    for ((v = value, i = index) in str.withIndex()) {
        append("$v:$i;")
    }
}

fun test4(str: String) = buildString {
    for ((index = value, value = index) in str.withIndex()) {
        append("$index:$value;")
    }
}

fun test5(str: String) = buildString {
    for ((a = index, b = index, c = value) in str.withIndex()) {
        append("$a:$b:$c;")
    }
}

fun test6(str: String) = buildString {
    for ((a = index, b = value, c = value) in str.withIndex()) {
        append("$a:$b:$c;")
    }
}

fun test7(str: String) = buildString {
    for ((value) in str.withIndex()) {
        append("$value;")
    }
}

fun box(): String {
    val s = "abc"

    assertEquals("0:a;1:b;2:c;", test1(s))
    assertEquals("a:0;b:1;c:2;", test2(s))
    assertEquals("a:0;b:1;c:2;", test3(s))
    assertEquals("a:0;b:1;c:2;", test4(s))
    assertEquals("0:0:a;1:1:b;2:2:c;", test5(s))
    assertEquals("0:a:a;1:b:b;2:c:c;", test6(s))
    assertEquals("a;b;c;", test7(s))

    return "OK"
}
