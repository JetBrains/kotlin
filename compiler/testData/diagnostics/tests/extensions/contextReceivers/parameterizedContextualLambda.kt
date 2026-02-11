// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// SKIP_TXT
// LANGUAGE: +ContextReceivers, -ContextParameters

fun <T> test(action: context(T) () -> Unit) {}

fun <T> test2(actionWithArg: context(T) (T) -> Unit) {}

fun main() {
    test<String> {
        length
    }
    test<Int> {
        toDouble()
    }
    test2<String> { a ->
        length
        a.length
    }
}

/* GENERATED_FIR_TAGS: functionDeclaration, functionalType, lambdaLiteral, nullableType, typeParameter, typeWithContext */
