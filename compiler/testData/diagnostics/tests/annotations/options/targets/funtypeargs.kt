target(AnnotationTarget.EXPRESSION) 
annotation class special

target(AnnotationTarget.TYPE) 
annotation class base

fun transform(i: Int, tr: (@<!DEBUG_INFO_MISSING_UNRESOLVED!>special<!> Int) -> Int): Int = @special tr(@special i)

fun foo(i: Int): Int {
    val j = @special i + 1
    if (j == 1) return foo(@special 42)
    return transform(@special j, @special { i: @base Int -> <!WRONG_ANNOTATION_TARGET!>@base<!> i * 2 })
}
