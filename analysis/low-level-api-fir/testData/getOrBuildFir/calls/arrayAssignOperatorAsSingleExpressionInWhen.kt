// LOOK_UP_FOR_ELEMENT_OF_TYPE: org.jetbrains.kotlin.psi.KtBinaryExpression

class Foo {
    operator fun set(n: Int, value: String) {}
}

fun usageFoo(foo: Foo) {
    when { else -> <expr>foo[10] = ""</expr> }
}

