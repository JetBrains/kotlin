// KT-9145

@Target(AnnotationTarget.CLASS)
annotation class Ann

var x: Int
    get() = 1
    set(<!WRONG_ANNOTATION_TARGET!>@Ann<!> private x) { }
