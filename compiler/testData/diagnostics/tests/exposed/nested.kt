// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
internal open class My

internal class Outer {
    // Ok, effectively internal from internal
    class Your: My()
}

/* GENERATED_FIR_TAGS: classDeclaration, nestedClass */
