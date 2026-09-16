// RUN_PIPELINE_TILL: CODEGEN
// DIAGNOSTICS: +UNUSED_PARAMETER
var y: Int = 1

// No backing field!
var x: Int
    get() = y
    set(field) {
        y = field
    }

/* GENERATED_FIR_TAGS: assignment, getter, integerLiteral, propertyDeclaration, setter */
