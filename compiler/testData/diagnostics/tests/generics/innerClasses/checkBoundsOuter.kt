// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
class Outer<E : Any> {
    inner class Inner<F, G>
}

val x: Outer<<!UPPER_BOUND_VIOLATED!>String?<!>>.Inner<String, Int> = null!!

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, inner, nullableType, propertyDeclaration, typeConstraint,
typeParameter */
