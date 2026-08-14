// RUN_PIPELINE_TILL: FRONTEND
// WITH_STDLIB
// DIAGNOSTICS: -UNUSED_VARIABLE

class Foo<T : Enum<T>>(val values: Array<T>)

fun foo(x: Array<out Enum<*>>) {
    val y = Foo(<!ARGUMENT_TYPE_MISMATCH!>x<!>)
}

/* GENERATED_FIR_TAGS: capturedType, classDeclaration, functionDeclaration, localProperty, outProjection,
primaryConstructor, propertyDeclaration, starProjection, typeConstraint, typeParameter */
