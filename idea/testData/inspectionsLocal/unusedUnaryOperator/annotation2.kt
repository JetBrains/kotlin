// PROBLEM: none

@Target(AnnotationTarget.FUNCTION)
annotation class Range(val min: Long = 0)

@Range(min = -<caret>90L)
fun foo(x: Int) {}