// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-72941

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class Ann

fun foo(y: Int) {
    var x = 1
    <!ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE!>@Ann x<!> += 2

    @Ann
    x += 2
}
