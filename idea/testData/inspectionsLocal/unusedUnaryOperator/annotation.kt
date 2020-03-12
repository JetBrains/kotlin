// PROBLEM: none

@Target(AnnotationTarget.VALUE_PARAMETER)
annotation class Range(val min: Long = 0)

fun foo(@Range(min = -<caret>90L) x: Int) {}