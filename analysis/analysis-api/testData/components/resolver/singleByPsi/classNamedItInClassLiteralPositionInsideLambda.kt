fun f() {
    class it

    42.let {
        val t = <expr>it</expr>::class
    }
}

// IGNORE_LOOKUP_LOCALLY
// LOOK_UP_FOR_ELEMENT_OF_TYPE: KtNameReferenceExpression
