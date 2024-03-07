// LOOK_UP_FOR_ELEMENT_OF_TYPE: org.jetbrains.kotlin.psi.KtBinaryExpression

class Foo {
    operator fun set(n: Int, value: String) {}
}

fun usageFoo(flag: Boolean, foo: Foo) {
    if (flag) <expr>foo[10] = ""</expr>
}

