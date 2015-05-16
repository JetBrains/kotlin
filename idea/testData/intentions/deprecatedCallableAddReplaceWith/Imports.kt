package pack

import dependency.valueFromOtherPackage

<caret>@deprecated("")
fun foo() {
    bar(valueFromOtherPackage)
}

fun bar(p: Int){}