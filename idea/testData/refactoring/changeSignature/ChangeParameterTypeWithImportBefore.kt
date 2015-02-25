package a

import b.foo

class Bar

fun test() {
    <caret>foo(Bar())
}