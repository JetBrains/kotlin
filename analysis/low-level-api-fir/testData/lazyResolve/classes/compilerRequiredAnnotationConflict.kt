object Some {
    @Target(AnnotationTarget.CLASS)
    annotation class A<caret>nn

    enum class AnnotationTarget {
        CLASS
    }
}
