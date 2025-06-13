// RUN_PIPELINE_TILL: FRONTEND
// MODULE: lib
// FILE: Lib.kt
@<!UNRESOLVED_REFERENCE!>ExposedCopyVisibility<!>
data class Foo private constructor(val x: Int) {
    companion object {
        fun new() = Foo(1)
    }
}

// MODULE: main(lib)
// KOTLINC_ARGS: -progressive
// PROGRESSIVE_MODE
// FILE: main.kt
fun main() {
    <!UNRESOLVED_REFERENCE!>Foo<!>.new().copy()
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, data, functionDeclaration, integerLiteral, objectDeclaration,
primaryConstructor, propertyDeclaration */
