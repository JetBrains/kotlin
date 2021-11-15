// WITH_STDLIB

import kotlin.test.assertEquals

fun test1(s1: String, s2: String, s3: String) =
        (s1 + s2) + s3

fun test2(s1: String, s2: String, s3: String) =
        s1 + (s2 + s3)

fun test3(s1: String, s2: String, s3: String, s4: String) =
        ((s1 + s2) + ((s3 + s4)))

fun test4(s1: String, s2: String, s3: String) =
        "s1: $s1; " +
        "s2: $s2; " +
        "s3: $s3"

fun test5(s1: String, s2: String, s3: String) =
        "${"s1:" + "${" " + s1};"} " +
        "${"s2:" + "${" " + s2};"} " +
        "${"s3:" + "${" " + s3}"}"

fun test6(s1: String, s2: String, s3: String) =
    "${"s1:" + "${" " + s1};"} ${"s2:" + "${" " + s2};"} ${"s3:" + "${" " + s3}"}"

fun test7(s1: String, s2: String, s3: String): String {
    fun foo(s: String) = s
    return "foo: " + foo(s1 + s2 + " ${foo("\${s3.length} = ${s3.length}")}")
}

fun box(): String {
    assertEquals("123", test1("1", "2", "3"))
    assertEquals("123", test2("1", "2", "3"))
    assertEquals("1234", test3("1", "2", "3", "4"))
    assertEquals("s1: 1; s2: 2; s3: 3", test4("1", "2", "3"))
    assertEquals("s1: 1; s2: 2; s3: 3", test5("1", "2", "3"))
    assertEquals("s1: 1; s2: 2; s3: 3", test6("1", "2", "3"))
    assertEquals("foo: 12 \${s3.length} = 1", test7("1", "2", "3"))
    return "OK"
}