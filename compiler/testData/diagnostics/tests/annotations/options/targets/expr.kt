annotation class base

@Target(AnnotationTarget.EXPRESSION) annotation class special

fun transform(i: Int, tr: (Int) -> Int): Int = <!WRONG_ANNOTATION_TARGET!>@base<!> @special tr(@special i)

@base <!WRONG_ANNOTATION_TARGET!>@special<!> fun foo(i: Int): Int {
    val j = <!WRONG_ANNOTATION_TARGET!>@base<!> @special i + 1
    if (j == 1) return foo(@special <!WRONG_ANNOTATION_TARGET!>@base<!> 42)
    return transform(@special j, @base @special { <!ANNOTATIONS_ON_BLOCK_LEVEL_EXPRESSION_ON_THE_SAME_LINE!>@special it<!> * 2 })
}
