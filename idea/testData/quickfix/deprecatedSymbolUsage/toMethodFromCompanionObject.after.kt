import dependency.C.Companion.newFun

// "Replace with 'newFun(this)'" "true"

fun foo(c: dependency.C) {
    newFun(c)
}