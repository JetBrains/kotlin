// "Replace with 'newFun(this)'" "true"

import dependency.C

fun foo(c: dependency.C) {
    C.<caret>newFun(c)
}