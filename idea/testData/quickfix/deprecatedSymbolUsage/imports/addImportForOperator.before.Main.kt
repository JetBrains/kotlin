// "Replace with 'newFun(n + listOf(s))'" "true"

import declaration.listOf
import declaration.oldFun

fun foo() {
    <caret>oldFun(listOf(2), 1)
}
