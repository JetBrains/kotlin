// ISSUE: KT-80243
// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm
// WITH_STDLIB

import kotlin.test.assertEquals

fun test1(cs: CharSequence) = buildString {
    for ((index, value) in cs.withIndex()) {
        append("$index:$value;")
    }
}

fun test2(cs: CharSequence) = buildString {
    for ((value, index) in cs.withIndex()) {
        append("$value:$index;")
    }
}

fun test3(cs: CharSequence) = buildString {
    for ((v = value, i = index) in cs.withIndex()) {
        append("$v:$i;")
    }
}

fun test4(cs: CharSequence) = buildString {
    for ((index = value, value = index) in cs.withIndex()) {
        append("$index:$value;")
    }
}

fun test5(cs: CharSequence) = buildString {
    for ((a = index, b = index, c = value) in cs.withIndex()) {
        append("$a:$b:$c;")
    }
}

fun test6(cs: CharSequence) = buildString {
    for ((a = index, b = value, c = value) in cs.withIndex()) {
        append("$a:$b:$c;")
    }
}

fun test7(cs: CharSequence) = buildString {
    for ((value) in cs.withIndex()) {
        append("$value;")
    }
}

fun test8(cs: CharSequence) = buildString {
    for ((index) in cs.withIndex()) {
        append("$index;")
    }
}

fun box(): String {
    val s: CharSequence = "abc"

    assertEquals("0:a;1:b;2:c;", test1(s))
    assertEquals("a:0;b:1;c:2;", test2(s))
    assertEquals("a:0;b:1;c:2;", test3(s))
    assertEquals("a:0;b:1;c:2;", test4(s))
    assertEquals("0:0:a;1:1:b;2:2:c;", test5(s))
    assertEquals("0:a:a;1:b:b;2:c:c;", test6(s))
    assertEquals("a;b;c;", test7(s))
    assertEquals("0;1;2;", test8(s))

    return "OK"
}
