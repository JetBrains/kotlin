// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtReferenceExpression
fun foo(action: Int.() -> Unit) {
    foo {
        <expr>this</expr>
    }
}
