// RUNTIME_WITH_KOTLIN_TEST

import kotlin.test.assertFalse

fun foo() {
    val a = "a"
    val b = "b"
    assertFalse<caret>(a === b)
}