// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers, -ContextParameters

<!CONTEXT_CLASS_OR_CONSTRUCTOR!>context<!>(T) class A<T>

<!CONTEXT_CLASS_OR_CONSTRUCTOR!>context<!>(Collection<P>) class B<P>

fun Int.foo() {
    A<Int>()
    A<String>()
}

fun Collection<Int>.bar() {
    B<Int>()
    B<String>()
}

/* GENERATED_FIR_TAGS: classDeclaration, funWithExtensionReceiver, functionDeclaration, nullableType, typeParameter */
