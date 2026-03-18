// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-12617

fun <T, K> Iterable<T>.groupFoldBy(keySelector: (T) -> K, operation: (T, K) -> T): Map<K, T> = TODO()
fun <T, K> Iterable<T>.groupFoldBy(keySelector: (T) -> K, initial: T, operation: (T, K) -> T): Map<K, T> = TODO()

// KT-12617: Cannot infer types in lambda parameter if multiple overloads are available
fun foo(values: Iterable<Int>) {
    values.<!NONE_APPLICABLE, NO_VALUE_FOR_PARAMETER!>groupFoldBy<!>({ <!UNRESOLVED_REFERENCE!>it<!> })
}

/* GENERATED_FIR_TAGS: funWithExtensionReceiver, functionDeclaration, functionalType, lambdaLiteral, nullableType,
typeParameter */
