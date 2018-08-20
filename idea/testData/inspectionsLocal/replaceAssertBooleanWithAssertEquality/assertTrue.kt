// RUNTIME_WITH_KOTLIN_TEST
// PROBLEM: none

import kotlin.test.*

fun foo() {
    val isA = true
    assertTrue<caret>(isA)
}