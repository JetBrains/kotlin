package foo

import foo.foo as bar

fun foo() {}

fun main() {
    foo.<caret>foo()
    foo.foo()
}
