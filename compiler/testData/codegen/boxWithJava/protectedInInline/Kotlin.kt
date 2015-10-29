package test

import JavaClass

class B : JavaClass() {
    inline fun bar() = FIELD
}

fun box() = B().bar()