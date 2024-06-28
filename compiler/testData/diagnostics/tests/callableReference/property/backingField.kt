// DIAGNOSTICS: -UNUSED_EXPRESSION

val i: Int = 10
    get() {
        ::<!UNSUPPORTED_REFERENCES_TO_VARIABLES_AND_PARAMETERS!>field<!>
        return field
    }
