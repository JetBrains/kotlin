// PROBLEM: none
package foo.bar

import foo.bar.MyEnum.*

enum class MyEnum(val id: Int) {
    A(1),
    B(2);

    companion object O {
        fun baz() = ""
    }
}

fun test() {
    <caret>MyEnum.O::baz
}