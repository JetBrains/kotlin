// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm
// WITH_STDLIB

import kotlin.test.assertEquals

fun test1(str: String) = buildString {
    for ((val index, val value) in str.withIndex()) {
        append("$index:$value;")
    }
}

fun test2(str: String) = buildString {
    for ((val value) in str.withIndex()) {
        append("$value;")
    }
}

fun test3(str: String) = buildString {
    for ((val index) in str.withIndex()) {
        append("$index;")
    }
}

fun test4(str: String) = buildString {
    for ((val i = index, val v = value) in str.withIndex()) {
        append("$i:$v;")
    }
}

fun test5(str: String) = buildString {
    for ((val i = index, val value) in str.withIndex()) {
        append("$i:$value;")
    }
}

fun test6(str: String) = buildString {
    for ((val index, val v = value) in str.withIndex()) {
        append("$index:$v;")
    }
}

fun test7(str: String) = buildString {
    for ((val v = value, val i = index) in str.withIndex()) {
        append("$i:$v;")
    }
}

fun box(): String {
    val s = "abc"

    assertEquals("0:a;1:b;2:c;", test1(s))
    assertEquals("a;b;c;", test2(s))
    assertEquals("0;1;2;", test3(s))
    assertEquals("0:a;1:b;2:c;", test4(s))
    assertEquals("0:a;1:b;2:c;", test5(s))
    assertEquals("0:a;1:b;2:c;", test6(s))
    assertEquals("0:a;1:b;2:c;", test7(s))

    return "OK"
}
