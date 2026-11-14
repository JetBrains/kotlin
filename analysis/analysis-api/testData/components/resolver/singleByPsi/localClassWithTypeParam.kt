fun <T> f() {
    class X<T> {
        val x: <expr>T</expr> = null!!
    }
}

// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtNameReferenceExpression
