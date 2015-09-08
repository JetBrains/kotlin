// !DIAGNOSTICS: -UNUSED_PARAMETER
@Target(AnnotationTarget.PROPERTY) annotation class base

<!WRONG_ANNOTATION_TARGET!>@base<!> annotation class derived

<!WRONG_ANNOTATION_TARGET!>@base<!> class correct(@base val x: Int, <!WRONG_ANNOTATION_TARGET!>@base<!> w: Int) {
    <!WRONG_ANNOTATION_TARGET!>@base<!> constructor(): this(0, 0)
}

<!WRONG_ANNOTATION_TARGET!>@base<!> enum class My {
    @base FIRST,
    @base SECOND
}

<!WRONG_ANNOTATION_TARGET!>@base<!> fun foo(<!WRONG_ANNOTATION_TARGET!>@base<!> y: <!WRONG_ANNOTATION_TARGET!>@base<!> Int): Int {
    <!WRONG_ANNOTATION_TARGET!>@base<!> fun bar(<!WRONG_ANNOTATION_TARGET!>@base<!> z: <!WRONG_ANNOTATION_TARGET!>@base<!> Int) = z + 1
    <!WRONG_ANNOTATION_TARGET!>@base<!> val local = bar(y)
    return local
}

@base val z = 0
