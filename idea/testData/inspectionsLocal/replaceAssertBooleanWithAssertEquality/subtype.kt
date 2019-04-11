// RUNTIME_WITH_KOTLIN_TEST

import kotlin.test.assertTrue

interface Parent

interface Child : Parent

fun test(p: Parent?, c: Child) {
    <caret>assertTrue(p == c)
}