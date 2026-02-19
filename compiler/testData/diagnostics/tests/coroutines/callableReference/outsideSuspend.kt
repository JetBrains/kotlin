// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE
// SKIP_TXT

import kotlin.reflect.KSuspendFunction0

suspend fun foo() {}

fun test() {
    ::foo checkType { _<KSuspendFunction0<Unit>>() }
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, funWithExtensionReceiver, functionDeclaration,
functionalType, infix, lambdaLiteral, nullableType, suspend, typeParameter, typeWithExtension */
