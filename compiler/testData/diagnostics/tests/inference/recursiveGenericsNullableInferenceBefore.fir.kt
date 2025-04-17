// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-76826
// LANGUAGE: -InferenceEnhancementsIn23
// LATEST_LV_DIFFERENCE
class Recursive<T1 : Recursive<T1>>

fun <T2 : Recursive<T2>> createRecursive(): T2 = TODO()

fun <T3 : Recursive<T3>> foo(): T3? {
    return <!TYPE_MISMATCH!><!UPPER_BOUND_VIOLATED_DEPRECATION_WARNING!>createRecursive<!>()<!>
}

fun <T3 : Recursive<T3 & Any>?> foo2(): T3 {
    return <!TYPE_MISMATCH!><!UPPER_BOUND_VIOLATED_DEPRECATION_WARNING!>createRecursive<!>()<!>
}

/* GENERATED_FIR_TAGS: classDeclaration, functionDeclaration, nullableType, typeConstraint, typeParameter */
