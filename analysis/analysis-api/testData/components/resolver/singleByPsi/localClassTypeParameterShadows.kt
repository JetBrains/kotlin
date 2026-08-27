fun <T> f() {
    class A<T>

    val x: <expr>T</expr> = null!!
}

// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtNameReferenceExpression
// ISSUE: KT-89161
