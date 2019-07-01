// RUNTIME_WITH_KOTLIN_TEST
// PROBLEM: none

import kotlin.test.assertTrue

interface Parent

interface Child : Parent

fun test(p: Parent, c: Child?) {
    assertTrue<caret>(c === p)
}