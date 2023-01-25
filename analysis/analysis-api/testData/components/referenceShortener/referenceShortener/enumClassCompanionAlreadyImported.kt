package a.b.c

import a.b.c.MyEnum.Companion.foo

enum class MyEnum(val id: Int) {
    A(1),
    B(2);

    companion object {
        fun foo() = ""
    }
}

fun test() {
    <expr>a.b.c.MyEnum.Companion.foo()</expr>
}

fun foo() = ""