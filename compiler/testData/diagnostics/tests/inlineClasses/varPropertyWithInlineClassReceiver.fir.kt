// !LANGUAGE: +InlineClasses
// !DIAGNOSTICS: -UNUSED_PARAMETER

inline class IC(val a: Any) {
    var member: Any
        get() = a
        set(value) {}
}

var IC.extension: Any
    get() = a
    set(value) {}
