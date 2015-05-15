// "Replace with 'newFun(this)'" "true"

fun foo(c: dependency.C) {
    c.<caret>oldFun()
}