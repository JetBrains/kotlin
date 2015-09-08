// !DIAGNOSTICS: -UNUSED_PARAMETER
@Target() annotation class empty

<!WRONG_ANNOTATION_TARGET!>@empty<!> annotation class derived

<!WRONG_ANNOTATION_TARGET!>@empty<!> class correct(<!WRONG_ANNOTATION_TARGET!>@empty<!> val x: Int, <!WRONG_ANNOTATION_TARGET!>@empty<!> w: <!WRONG_ANNOTATION_TARGET!>@empty<!> Int) {
    <!WRONG_ANNOTATION_TARGET!>@empty<!> constructor(): this(0, 0)
}

<!WRONG_ANNOTATION_TARGET!>@empty<!> enum class My <!WRONG_ANNOTATION_TARGET!>@empty<!> constructor() {
    <!WRONG_ANNOTATION_TARGET!>@empty<!> FIRST,
    <!WRONG_ANNOTATION_TARGET!>@empty<!> SECOND
}

<!WRONG_ANNOTATION_TARGET!>@empty<!> fun foo(<!WRONG_ANNOTATION_TARGET!>@empty<!> y: <!WRONG_ANNOTATION_TARGET!>@empty<!> Int): Int {
    <!WRONG_ANNOTATION_TARGET!>@empty<!> fun bar(<!WRONG_ANNOTATION_TARGET!>@empty<!> z: <!WRONG_ANNOTATION_TARGET!>@empty<!> Int) = z + 1
    <!WRONG_ANNOTATION_TARGET!>@empty<!> val local = bar(y)
    return local
}

<!WRONG_ANNOTATION_TARGET!>@empty<!> val z = <!WRONG_ANNOTATION_TARGET!>@empty<!> 0
