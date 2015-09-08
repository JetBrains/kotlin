// !DIAGNOSTICS: -UNUSED_PARAMETER
@Target(AnnotationTarget.<!UNRESOLVED_REFERENCE!>INIT<!>) annotation class incorrect

<!WRONG_ANNOTATION_TARGET!>@incorrect<!> annotation class derived

<!WRONG_ANNOTATION_TARGET!>@incorrect<!> class correct(<!WRONG_ANNOTATION_TARGET!>@incorrect<!> val x: Int, <!WRONG_ANNOTATION_TARGET!>@incorrect<!> w: <!WRONG_ANNOTATION_TARGET!>@incorrect<!> Int) {
    <!WRONG_ANNOTATION_TARGET!>@incorrect<!> constructor(): this(0, 0)
}

<!WRONG_ANNOTATION_TARGET!>@incorrect<!> enum class My <!WRONG_ANNOTATION_TARGET!>@incorrect<!> constructor() {
    <!WRONG_ANNOTATION_TARGET!>@incorrect<!> FIRST,
    <!WRONG_ANNOTATION_TARGET!>@incorrect<!> SECOND
}

<!WRONG_ANNOTATION_TARGET!>@incorrect<!> fun foo(<!WRONG_ANNOTATION_TARGET!>@incorrect<!> y: <!WRONG_ANNOTATION_TARGET!>@incorrect<!> Int): Int {
    <!WRONG_ANNOTATION_TARGET!>@incorrect<!> fun bar(<!WRONG_ANNOTATION_TARGET!>@incorrect<!> z: <!WRONG_ANNOTATION_TARGET!>@incorrect<!> Int) = z + 1
    <!WRONG_ANNOTATION_TARGET!>@incorrect<!> val local = bar(y)
    return local
}

<!WRONG_ANNOTATION_TARGET!>@incorrect<!> val z = <!WRONG_ANNOTATION_TARGET!>@incorrect<!> 0
