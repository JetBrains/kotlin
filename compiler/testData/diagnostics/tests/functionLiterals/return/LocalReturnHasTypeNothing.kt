// RUN_PIPELINE_TILL: FRONTEND
// CHECK_TYPE

fun test() {
    run f@{
        checkSubtype<Nothing>(return@f 1)
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType, infix,
integerLiteral, lambdaLiteral, nullableType, typeParameter, typeWithExtension */
