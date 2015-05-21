// "Replace with 's.extension1().extension2'" "true"

import dependency.oldFun
import dependency2.extension1
import dependency2.extension2

fun foo() {
    "a".extension1().<caret>extension2
}