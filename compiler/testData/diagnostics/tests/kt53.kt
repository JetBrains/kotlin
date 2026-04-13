// RUN_PIPELINE_TILL: BACKEND
val <T> T.foo : T?
    get() = null

fun test(): Int? {
    return 1.foo
}

/* GENERATED_FIR_TAGS: functionDeclaration, getter, integerLiteral, nullableType, propertyDeclaration,
propertyWithExtensionReceiver, typeParameter */
