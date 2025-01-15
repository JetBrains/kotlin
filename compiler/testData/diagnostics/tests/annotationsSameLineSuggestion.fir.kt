// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-72941

@Target(AnnotationTarget.EXPRESSION)
@Retention(AnnotationRetention.SOURCE)
annotation class Ann

fun foo(y: Int) {
    var x = 1
    @Ann x <!UNRESOLVED_REFERENCE!>+=<!> 2

    @Ann
    x += 2
}
