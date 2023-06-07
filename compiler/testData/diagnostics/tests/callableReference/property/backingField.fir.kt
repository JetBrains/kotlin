// !DIAGNOSTICS: -UNUSED_EXPRESSION

val i: Int = 10
    get() {
        ::<!UNSUPPORTED!>field<!>
        return field
    }