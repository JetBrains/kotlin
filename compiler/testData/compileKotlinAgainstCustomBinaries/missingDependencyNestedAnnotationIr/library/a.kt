package a

interface A {
    @Target(AnnotationTarget.CLASS, AnnotationTarget.FUNCTION, AnnotationTarget.TYPE_PARAMETER)
    annotation class Anno(val value: String)
}
