// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtReferenceExpression
interface A {
    operator fun plusAssign(i: Int)
}

fun foo(action: A.() -> Unit) {
    foo {
        <expr>this</expr> += 1
    }
}
