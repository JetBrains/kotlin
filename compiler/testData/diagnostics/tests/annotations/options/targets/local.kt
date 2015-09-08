@Target(AnnotationTarget.LOCAL_VARIABLE) annotation class base

<!WRONG_ANNOTATION_TARGET!>@base<!> annotation class derived

<!WRONG_ANNOTATION_TARGET!>@base<!> class correct(<!WRONG_ANNOTATION_TARGET!>@base<!> val x: Int) {
    <!WRONG_ANNOTATION_TARGET!>@base<!> constructor(): this(0)
}

<!WRONG_ANNOTATION_TARGET!>@base<!> enum class My {
    <!WRONG_ANNOTATION_TARGET!>@base<!> FIRST,
    <!WRONG_ANNOTATION_TARGET!>@base<!> SECOND
}

<!WRONG_ANNOTATION_TARGET!>@base<!> fun foo(<!WRONG_ANNOTATION_TARGET!>@base<!> y: <!WRONG_ANNOTATION_TARGET!>@base<!> Int): Int {
    <!WRONG_ANNOTATION_TARGET!>@base<!> fun bar(<!WRONG_ANNOTATION_TARGET!>@base<!> z: <!WRONG_ANNOTATION_TARGET!>@base<!> Int) = z + 1
    @base val local = bar(y)
    return local
}

<!WRONG_ANNOTATION_TARGET!>@base<!> val z = 0
