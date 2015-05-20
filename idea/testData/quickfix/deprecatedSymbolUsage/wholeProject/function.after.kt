// "Replace usages of 'oldFun(Int): Unit' in whole project" "true"

import newPack.newFun
import pack.oldFun

fun foo() {
    <caret>newFun(0 + 1)
}
