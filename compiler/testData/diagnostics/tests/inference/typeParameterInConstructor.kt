// RUN_PIPELINE_TILL: BACKEND

class B<O>(val obj: O) {
    val v = B(obj)
}

/* GENERATED_FIR_TAGS: classDeclaration, nullableType, primaryConstructor, propertyDeclaration, typeParameter */
