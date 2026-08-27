fun <T> f() {
    class X<T> {
        val x: <expr>T</expr> = null!!
    }
}

// IGNORE_LOOKUP_LOCALLY
// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtNameReferenceExpression
