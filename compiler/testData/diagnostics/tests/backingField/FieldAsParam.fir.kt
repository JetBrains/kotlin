// DIAGNOSTICS: +UNUSED_PARAMETER
var y: Int = 1

// No backing field!
var x: Int
    get() = y
    set(<!ACCESSOR_PARAMETER_NAME_SHADOWING!>field<!>) {
        y = field
    }
