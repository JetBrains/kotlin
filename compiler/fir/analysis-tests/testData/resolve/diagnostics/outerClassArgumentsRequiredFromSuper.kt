// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-82085

open class X<T> {
    open inner class Y
}

class A : X<String>() {
    class D<U : <!OUTER_CLASS_ARGUMENTS_REQUIRED!>Y<!>>
}

/* GENERATED_FIR_TAGS: classDeclaration, inner, nestedClass, nullableType, typeConstraint, typeParameter */
