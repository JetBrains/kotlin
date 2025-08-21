// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-74929

val <T : Number?> (T & Any).prop : T get() = this

interface Base {
    val <T : Number?> (T & Any).prop : T

    val <T : Number?> List<T & Any>.prop : T
}

/* GENERATED_FIR_TAGS: dnnType, getter, interfaceDeclaration, nullableType, propertyDeclaration,
propertyWithExtensionReceiver, thisExpression, typeConstraint, typeParameter */
