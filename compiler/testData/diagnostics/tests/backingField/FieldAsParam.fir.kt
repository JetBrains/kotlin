// RUN_PIPELINE_TILL: BACKEND
// DIAGNOSTICS: +UNUSED_PARAMETER
var y: Int = 1

// No backing field!
var x: Int
    get() = y
    set(field) {
        y = field
    }
