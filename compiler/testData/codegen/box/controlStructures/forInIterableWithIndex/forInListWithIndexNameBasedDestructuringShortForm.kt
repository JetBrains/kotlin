// ISSUE: KT-80243
// IGNORE_BACKEND_K1: ANY
// LANGUAGE: +NameBasedDestructuring, +EnableNameBasedDestructuringShortForm
// WITH_STDLIB

import kotlin.test.assertEquals

fun test1(list: List<String>) = buildString {
    for ((index, value) in list.withIndex()) {
        append("$index:$value;")
    }
}

fun test2(list: List<String>) = buildString {
    for ((value, index) in list.withIndex()) {
        append("$value:$index;")
    }
}

fun test3(list: List<String>) = buildString {
    for ((v = value, i = index) in list.withIndex()) {
        append("$v:$i;")
    }
}

fun test4(list: List<String>) = buildString {
    for ((index = value, value = index) in list.withIndex()) {
        append("$index:$value;")
    }
}

fun test5(list: List<String>) = buildString {
    for ((a = index, b = index, c = value) in list.withIndex()) {
        append("$a:$b:$c;")
    }
}

fun test6(list: List<String>) = buildString {
    for ((a = index, b = value, c = value) in list.withIndex()) {
        append("$a:$b:$c;")
    }
}

fun test7(list: List<String>) = buildString {
    for ((value) in list.withIndex()) {
        append("$value;")
    }
}

fun test8(list: List<String>) = buildString {
    for ((index) in list.withIndex()) {
        append("$index;")
    }
}

fun box(): String {
    val list = listOf("a", "b", "c")

    assertEquals("0:a;1:b;2:c;", test1(list))
    assertEquals("a:0;b:1;c:2;", test2(list))
    assertEquals("a:0;b:1;c:2;", test3(list))
    assertEquals("a:0;b:1;c:2;", test4(list))
    assertEquals("0:0:a;1:1:b;2:2:c;", test5(list))
    assertEquals("0:a:a;1:b:b;2:c:c;", test6(list))
    assertEquals("a;b;c;", test7(list))
    assertEquals("0;1;2;", test8(list))

    return "OK"
}
