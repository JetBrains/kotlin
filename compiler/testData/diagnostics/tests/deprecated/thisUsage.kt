// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE

@Deprecated("Use A instead") open class MyClass {
    fun foo() {
        val test = this
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, localProperty, propertyDeclaration, stringLiteral,
thisExpression */
