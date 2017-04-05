// "Replace with 'newFun(p, p)'" "true"

import java.util.*

@Deprecated("", ReplaceWith("newFun(p, p)"))
fun oldFun(p: List<String>) {
    newFun(p, p)
}

fun newFun(p1: List<String>, p2: List<String>){}

fun foo() {
    <caret>oldFun(bar())
}

fun <T> bar(): List<T> = ArrayList()
