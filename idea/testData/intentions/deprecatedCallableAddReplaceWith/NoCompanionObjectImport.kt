package pack

import dependency.D

<caret>@deprecated("")
fun foo() {
    bar(D.value)
}

fun bar(p: Int){}