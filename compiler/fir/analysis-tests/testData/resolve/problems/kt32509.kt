// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-32509

// KT-32509: No capture from lambda result
interface Inv<T>

fun getInvWithOut(): Inv<out String> = TODO()

fun <T> captureFromLambda(f: () -> Inv<T>): Inv<T> = TODO()
fun <T> captureFromArgument(a: Inv<T>): Inv<T> = TODO()

fun main(args: Array<String>) {
    val x = captureFromLambda { getInvWithOut() } // error: type inference failed
    val y = captureFromArgument(getInvWithOut()) // no error -- correct capturing from argument
}

/* GENERATED_FIR_TAGS: capturedType, functionDeclaration, functionalType, interfaceDeclaration, lambdaLiteral,
localProperty, nullableType, outProjection, propertyDeclaration, typeParameter */
