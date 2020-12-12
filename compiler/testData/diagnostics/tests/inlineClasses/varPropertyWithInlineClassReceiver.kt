// !LANGUAGE: +InlineClasses
// !DIAGNOSTICS: -UNUSED_PARAMETER

inline class IC(val a: Any) {
    <!RESERVED_VAR_PROPERTY_OF_VALUE_CLASS!>var<!> member: Any
        get() = a
        set(value) {}
}

<!RESERVED_VAR_PROPERTY_OF_VALUE_CLASS!>var<!> IC.extension: Any
    get() = a
    set(value) {}