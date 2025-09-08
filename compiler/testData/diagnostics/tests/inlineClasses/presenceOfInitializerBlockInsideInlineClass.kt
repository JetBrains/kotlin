// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// LANGUAGE: +InlineClasses
// DIAGNOSTICS: -UNUSED_VARIABLE, -INLINE_CLASS_DEPRECATED

inline class Foo(val x: Int) {
    init {}

    init {
        val f = 1
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, init, integerLiteral, localProperty, primaryConstructor, propertyDeclaration */
