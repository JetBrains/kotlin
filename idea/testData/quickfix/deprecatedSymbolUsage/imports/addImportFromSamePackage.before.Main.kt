// "Replace with 's.newFun()'" "true"

import dependency.oldFun

fun foo() {
    <caret>oldFun("a")
}