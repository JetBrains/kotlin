// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -INLINE_CLASS_DEPRECATED -INLINE_CLASS_DEPRECATED_ERROR

inline class Foo(val x: Int)

<!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var a: Foo

fun foo() {
    <!INAPPLICABLE_LATEINIT_MODIFIER!>lateinit<!> var b: Foo
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, lateinit, localProperty, primaryConstructor,
propertyDeclaration */
