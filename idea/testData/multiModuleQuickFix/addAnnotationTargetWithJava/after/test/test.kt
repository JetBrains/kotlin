package test

@Target(AnnotationTarget.EXPRESSION, AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.LOCAL_VARIABLE, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
annotation class AnnTarget

fun apply() {
    var v = 0
    @AnnTarget v++
    println(v)
}