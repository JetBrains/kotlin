// DIAGNOSTICS: -UNUSED_PARAMETER

@Target(AnnotationTarget.FUNCTION, AnnotationTarget.TYPE)
annotation class Composable

fun bar(p: @Composable ()->Unit) {}

@Composable fun foo() {}

fun main() {
    bar(<!DEBUG_INFO_EXPRESSION_TYPE("@Composable () -> kotlin.Unit")!>if(true) { { foo() } } else { { } }<!>)
}
