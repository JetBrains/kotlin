// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

fun arrayAccessRHS(a: Int?, b: Array<Int>) {
    b[0] = a!!
    checkSubtype<Int>(a)
}

fun arrayAccessLHS(a: Int?, b: Array<Int>) {
    b[a!!] = a
    checkSubtype<Int>(a)
}

/* GENERATED_FIR_TAGS: assignment, checkNotNullCall, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionalType, infix, integerLiteral, nullableType, smartcast, typeParameter, typeWithExtension */
