// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// SKIP_TXT
// LANGUAGE: +ContextReceivers, -ContextParameters

fun <T> test(action: context(T) () -> Unit) {}

fun <T> test2(actionWithArg: context(T) (T) -> Unit) {}

fun main() {
    test<String> {
        <!UNRESOLVED_REFERENCE!>length<!>
    }
    test<Int> {
        <!UNRESOLVED_REFERENCE!>toDouble<!>()
    }
    test2<String> { a ->
        <!UNRESOLVED_REFERENCE!>length<!>
        a.length
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, nullableType, typeParameter, typeWithContext */
