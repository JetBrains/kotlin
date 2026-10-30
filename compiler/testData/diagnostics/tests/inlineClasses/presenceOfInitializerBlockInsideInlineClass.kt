// RUN_PIPELINE_TILL: CODEGEN

@file:Suppress("INLINE_CLASS_DEPRECATED", <!ERROR_SUPPRESSION!>"INLINE_CLASS_DEPRECATED_ERROR"<!>)

inline class Foo(val x: Int) {
    init {}

    init {
        val f = 1
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, init, integerLiteral, localProperty, primaryConstructor, propertyDeclaration */
