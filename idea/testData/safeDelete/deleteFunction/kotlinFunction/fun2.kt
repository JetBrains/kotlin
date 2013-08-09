package test

import test.foo

fun <caret>foo() {

}

class B {
    val ref = ::foo
}