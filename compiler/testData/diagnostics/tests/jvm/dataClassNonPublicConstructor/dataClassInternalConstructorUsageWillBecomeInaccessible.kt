// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB
// MODULE: lib
// FILE: Lib.kt
@ExposedCopyVisibility
data class Foo private constructor(val x: Int) {
    companion object {
        fun new() = Foo(1)
    }
}

// MODULE: main(lib)
// PROGRESSIVE_MODE
// FILE: main.kt
fun main() {
    Foo.new().<!DATA_CLASS_INVISIBLE_COPY_USAGE_WARNING!>copy<!>()
}

/* GENERATED_FIR_TAGS: classDeclaration, companionObject, data, functionDeclaration, integerLiteral, objectDeclaration,
primaryConstructor, propertyDeclaration */
