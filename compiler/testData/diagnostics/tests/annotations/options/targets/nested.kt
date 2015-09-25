@Target(AnnotationTarget.CLASS)
annotation class base

@Target(AnnotationTarget.ANNOTATION_CLASS)
annotation class meta

@base class Outer {
    @base <!WRONG_ANNOTATION_TARGET!>@meta<!> class Nested

    @base @meta annotation class Annotated

    fun foo() {
        @base <!WRONG_ANNOTATION_TARGET!>@meta<!> class Local
    }
}
