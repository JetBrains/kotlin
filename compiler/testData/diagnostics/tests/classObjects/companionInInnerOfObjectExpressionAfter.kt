// LANGUAGE: +ForbidCompanionInLocalInnerClass
val x = object {
    inner class D {
        companion object
    }
}