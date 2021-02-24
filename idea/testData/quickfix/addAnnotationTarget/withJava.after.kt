// "Add annotation target" "true"
// ERROR: This annotation is not applicable to target 'expression'

package test

@Target(
    AnnotationTarget.CLASS,
    AnnotationTarget.LOCAL_VARIABLE,
    AnnotationTarget.VALUE_PARAMETER,
    AnnotationTarget.CONSTRUCTOR,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.EXPRESSION
)
@Retention(AnnotationRetention.SOURCE)
annotation class AnnTarget

fun println(v: Int) {}

fun apply() {
    var v = 0
    <caret>@AnnTarget v++
    println(v)
}