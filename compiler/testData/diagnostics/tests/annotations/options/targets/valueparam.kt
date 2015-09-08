// !DIAGNOSTICS: -UNUSED_PARAMETER
@Target(AnnotationTarget.VALUE_PARAMETER) annotation class base

<!WRONG_ANNOTATION_TARGET!>@base<!> annotation class derived

<!WRONG_ANNOTATION_TARGET!>@base<!> class correct(@base val x: Int, @base w: Int) {
    <!WRONG_ANNOTATION_TARGET!>@base<!> constructor(): this(0, 0)
}

<!WRONG_ANNOTATION_TARGET!>@base<!> enum class My {
    <!WRONG_ANNOTATION_TARGET!>@base<!> FIRST,
    <!WRONG_ANNOTATION_TARGET!>@base<!> SECOND
}

<!WRONG_ANNOTATION_TARGET!>@base<!> fun foo(@base y: <!WRONG_ANNOTATION_TARGET!>@base<!> Int): Int {
    <!WRONG_ANNOTATION_TARGET!>@base<!> fun bar(@base z: <!WRONG_ANNOTATION_TARGET!>@base<!> Int) = z + 1
    <!WRONG_ANNOTATION_TARGET!>@base<!> val local = bar(y)
    return local
}

<!WRONG_ANNOTATION_TARGET!>@base<!> val z = 0
