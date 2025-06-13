// RUN_PIPELINE_TILL: FRONTEND
// FIR_IDENTICAL
// CHECK_TYPE

interface Predicate<T>

fun <T> Predicate(x: (T?) -> Boolean): Predicate<T> = null!!

fun foo() {
    process(Predicate {
        x -> x checkType { _<String?>() }

        true
    })
}

fun process(x: Predicate<String>) {}

/* GENERATED_FIR_TAGS: checkNotNullCall, classDeclaration, funWithExtensionReceiver, functionDeclaration, functionalType,
infix, interfaceDeclaration, lambdaLiteral, nullableType, typeParameter, typeWithExtension */
