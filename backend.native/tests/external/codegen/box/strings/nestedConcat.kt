// WITH_RUNTIME

import kotlin.test.assertEquals

fun test1(s1: String, s2: String, s3: String) =
        (s1 + s2) + s3

fun test2(s1: String, s2: String, s3: String) =
        s1 + (s2 + s3)

fun test3(s1: String, s2: String, s3: String) =
        "s1: $s1; " +
        "s2: $s2; " +
        "s3: $s3"

fun box(): String {
    assertEquals("123", test1("1", "2", "3"))
    assertEquals("123", test2("1", "2", "3"))
    assertEquals("s1: 1; s2: 2; s3: 3", test3("1", "2", "3"))
    return "OK"
}