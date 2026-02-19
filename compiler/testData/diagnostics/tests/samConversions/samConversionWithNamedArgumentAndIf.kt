// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// ISSUE: KT-67311

fun interface Callable<in T, out R> {
    operator fun invoke(arg: T): R
}

fun<T> findMessages(msg: Callable<T, String?>) {}

fun runTest() {
    findMessages<Int>(
        msg = if (true) {
            { "a" }
        } else {
            { "b" }
        }
    )
}

/* GENERATED_FIR_TAGS: funInterface, functionDeclaration, ifExpression, in, interfaceDeclaration, lambdaLiteral,
nullableType, operator, out, samConversion, stringLiteral, typeParameter */
