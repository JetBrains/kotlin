package pack

import dependency.D

<caret>@Deprecated("")
fun foo() {
    bar(D.value)
}

fun bar(p: Int){}