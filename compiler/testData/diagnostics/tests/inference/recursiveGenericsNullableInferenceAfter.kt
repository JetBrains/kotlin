// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-76826
// LANGUAGE: +InferenceEnhancementsIn23, +DontIgnoreUpperBoundViolatedOnImplicitArguments
// FIR_IDENTICAL

class Recursive<T1 : Recursive<T1>>

fun <T2 : Recursive<T2>> createRecursive(): T2 = TODO()

fun <T3 : Recursive<T3>> foo(): T3? {
    return createRecursive()
}

fun <T3 : Recursive<T3 & Any>?> foo2(): T3 {
    return createRecursive()
}

/* GENERATED_FIR_TAGS: classDeclaration, dnnType, functionDeclaration, nullableType, typeConstraint, typeParameter */
