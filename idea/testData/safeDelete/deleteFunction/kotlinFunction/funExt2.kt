package test

import test.foo

class B {
    val ref = ::foo
}

fun B.<caret>foo() {

}
