// IS_APPLICABLE: false
package foo.bar

import foo.bar.A.SUCCESS

fun test() {
    foo(SUCCESS)
    bar(B.SUCCESS<caret>)
}

fun foo(a: A) {}

fun bar(b: B) {}

enum class A {
    SUCCESS, ERROR
}

enum class B {
    SUCCESS, ERROR
}

