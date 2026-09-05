class X<T> {
    context(_: T)
    val <T> x: Int
        get() {
            val a: <expr>T</expr> = null!!
            return 0
        }
}

// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtNameReferenceExpression
// ISSUE: KT-89148
