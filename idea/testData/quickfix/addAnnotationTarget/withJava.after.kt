// "Add annotation target" "true"
// ERROR: This annotation is not applicable to target 'expression'

package test

@Target(AnnotationTarget.EXPRESSION, AnnotationTarget.CLASS, AnnotationTarget.CONSTRUCTOR, AnnotationTarget.LOCAL_VARIABLE, AnnotationTarget.FUNCTION, AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class AnnTarget

fun println(v: Int) {}

fun apply() {
    var v = 0
    @AnnTarget v++
    println(v)
}