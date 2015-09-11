// !DIAGNOSTICS: -UNUSED_PARAMETER
@Target(AnnotationTarget.CLASS) annotation class base

@base annotation class derived

@base class correct(<!WRONG_ANNOTATION_TARGET!>@base<!> val x: Int, <!WRONG_ANNOTATION_TARGET!>@base<!> w: <!WRONG_ANNOTATION_TARGET!>@base<!> Int) {
    <!WRONG_ANNOTATION_TARGET!>@base<!> constructor(): this(0, 0)
}

@base enum class My <!WRONG_ANNOTATION_TARGET!>@base<!> constructor() {
    <!WRONG_ANNOTATION_TARGET!>@base<!> FIRST,
    <!WRONG_ANNOTATION_TARGET!>@base<!> SECOND
}

<!WRONG_ANNOTATION_TARGET!>@base<!> fun foo(<!WRONG_ANNOTATION_TARGET!>@base<!> y: <!WRONG_ANNOTATION_TARGET!>@base<!> Int): Int {
    <!WRONG_ANNOTATION_TARGET!>@base<!> fun bar(<!WRONG_ANNOTATION_TARGET!>@base<!> z: <!WRONG_ANNOTATION_TARGET!>@base<!> Int) = z + 1
    <!WRONG_ANNOTATION_TARGET!>@base<!> val local = bar(y)
    return local
}

<!WRONG_ANNOTATION_TARGET!>@base<!> val z = 0
