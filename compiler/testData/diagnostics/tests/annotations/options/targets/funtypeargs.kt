@Target(AnnotationTarget.EXPRESSION)
annotation class special

@Target(AnnotationTarget.TYPE)
annotation class base

fun transform(i: Int, tr: (@<!DEBUG_INFO_MISSING_UNRESOLVED!>special<!> Int) -> Int): Int = @special tr(@special i)

fun foo(i: Int): Int {
    val j = @special i + 1
    if (j == 1) return foo(@special 42)
    return transform(@special j, @special { i: @base Int -> <!ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE!><!WRONG_ANNOTATION_TARGET!>@base<!> i<!> * 2 })
}
