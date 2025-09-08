// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_PARAMETER

class R<T: R<T>>

open class Base<T> {
    fun foo(r: R<*>) {}
}

class Derived: Base<String>()

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, starProjection, typeConstraint,
typeParameter */
