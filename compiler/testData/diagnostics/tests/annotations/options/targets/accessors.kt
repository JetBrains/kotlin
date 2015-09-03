target(AnnotationTarget.PROPERTY_GETTER)
annotation class smartget

target(AnnotationTarget.PROPERTY_SETTER)
annotation class smartset

target(AnnotationTarget.FUNCTION)
annotation class base

class My(x: Int) {
    smartget var y = x
    <!WRONG_ANNOTATION_TARGET!>@base<!> @smartget <!WRONG_ANNOTATION_TARGET!>@smartset<!> get
    <!WRONG_ANNOTATION_TARGET!>@base<!> <!WRONG_ANNOTATION_TARGET!>@smartget<!> @smartset set

    base <!WRONG_ANNOTATION_TARGET!>smartget<!> <!WRONG_ANNOTATION_TARGET!>smartset<!> fun foo() = y
}