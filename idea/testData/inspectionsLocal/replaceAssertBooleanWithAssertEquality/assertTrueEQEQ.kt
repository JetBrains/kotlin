// RUNTIME_WITH_KOTLIN_TEST

import kotlin.test.*

fun foo() {
    val a = "a"
    val b = "a"
    assertTrue<caret>(a == b)
}