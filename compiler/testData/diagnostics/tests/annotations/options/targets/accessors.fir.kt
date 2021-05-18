@Target(AnnotationTarget.PROPERTY_GETTER)
annotation class smartget

@Target(AnnotationTarget.PROPERTY_SETTER)
annotation class smartset

@Target(AnnotationTarget.FUNCTION)
annotation class base

class My(x: Int) {
    <!WRONG_ANNOTATION_TARGET!>@smartget<!> var y = x
    @base @smartget @smartset get
    @base @smartget @smartset set

    @base <!WRONG_ANNOTATION_TARGET!>@smartget<!> <!WRONG_ANNOTATION_TARGET!>@smartset<!> fun foo() = y
}