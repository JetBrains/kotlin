// ISSUE: KT-80243
// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_SECOND_PHASE: 2.2.0
// WITH_STDLIB

import kotlin.test.assertEquals

fun test1(seq: Sequence<String>) = buildString {
    for ((index, value) in seq.withIndex()) {
        append("$index:$value;")
    }
}

fun test2(seq: Sequence<String>) = buildString {
    for ((value, index) in seq.withIndex()) {
        append("$value:$index;")
    }
}

fun test3(seq: Sequence<String>) = buildString {
    for ((v = value, i = index) in seq.withIndex()) {
        append("$v:$i;")
    }
}

fun test4(seq: Sequence<String>) = buildString {
    for ((index = value, value = index) in seq.withIndex()) {
        append("$index:$value;")
    }
}

fun test5(seq: Sequence<String>) = buildString {
    for ((a = index, b = index, c = value) in seq.withIndex()) {
        append("$a:$b:$c;")
    }
}

fun test6(seq: Sequence<String>) = buildString {
    for ((a = index, b = value, c = value) in seq.withIndex()) {
        append("$a:$b:$c;")
    }
}

fun test7(seq: Sequence<String>) = buildString {
    for ((value) in seq.withIndex()) {
        append("$value;")
    }
}

fun box(): String {
    val seq = sequenceOf("a", "b", "c")

    assertEquals("0:a;1:b;2:c;", test1(seq))
    assertEquals("a:0;b:1;c:2;", test2(seq))
    assertEquals("a:0;b:1;c:2;", test3(seq))
    assertEquals("a:0;b:1;c:2;", test4(seq))
    assertEquals("0:0:a;1:1:b;2:2:c;", test5(seq))
    assertEquals("0:a:a;1:b:b;2:c:c;", test6(seq))
    assertEquals("a;b;c;", test7(seq))

    return "OK"
}
