// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE

val Any?.meaning: Int
    get() = 42

fun test() {
    val f = Any?::meaning
    checkSubtype<Int>(f.get(null))
    checkSubtype<Int>(f.get(""))
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionalType, getter, infix, integerLiteral, localProperty, nullableType, propertyDeclaration,
propertyWithExtensionReceiver, stringLiteral, typeParameter, typeWithExtension */
