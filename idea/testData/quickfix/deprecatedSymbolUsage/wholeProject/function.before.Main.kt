// "Replace usages of 'oldFun(Int): Unit' in whole project" "true"

import pack.oldFun

fun foo() {
    <caret>oldFun(0)
}
