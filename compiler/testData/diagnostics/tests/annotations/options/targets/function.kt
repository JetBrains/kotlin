@Target(AnnotationTarget.FUNCTION) annotation class base

<!WRONG_ANNOTATION_TARGET!>@base<!> annotation class derived

<!WRONG_ANNOTATION_TARGET!>@base<!> class correct(<!WRONG_ANNOTATION_TARGET!>@base<!> val x: Int) {
    <!WRONG_ANNOTATION_TARGET!>@base<!> constructor(): this(0)

    @base public fun baz() {}
}

<!WRONG_ANNOTATION_TARGET!>@base<!> enum class My <!WRONG_ANNOTATION_TARGET!>@base<!> constructor() {
    <!WRONG_ANNOTATION_TARGET!>@base<!> FIRST,
    <!WRONG_ANNOTATION_TARGET!>@base<!> SECOND
}

@base fun foo(<!WRONG_ANNOTATION_TARGET!>@base<!> y: <!WRONG_ANNOTATION_TARGET!>@base<!> Int): Int {
    @base fun bar(<!WRONG_ANNOTATION_TARGET!>@base<!> z: <!WRONG_ANNOTATION_TARGET!>@base<!> Int) = z + 1
    <!WRONG_ANNOTATION_TARGET!>@base<!> val local = bar(y)
    return local
}

<!WRONG_ANNOTATION_TARGET!>@base<!> val z = 0