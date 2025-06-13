// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
open class X<T>

class A: X<A.B>() {
    class B
}

/* GENERATED_FIR_TAGS: classDeclaration, nestedClass, nullableType, typeParameter */
