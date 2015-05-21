// "Replace with 's.extension().newFun()'" "true"

import dependency.oldFun

fun foo() {
    <caret>oldFun("a")
}