// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-49021

// KT-49021: Investigate rules of computations under NewResolvedCallImpl.getExpectedTypeForSamConvertedArgument
// The method can return null; here we test that fun interface SAM conversion works correctly
// when a Kotlin function type is passed where a fun interface is expected.

fun interface MyFunInterface {
    fun invoke(): Int
}

fun takeFunInterface(f: MyFunInterface): Int = f.invoke()

fun main() {
    // Direct lambda - simplest SAM conversion
    val r1 = takeFunInterface { 42 }

    // Function type variable passed as fun interface - SAM conversion from function type
    val fn: () -> Int = { 42 }
    val r2 = takeFunInterface(fn)
}

/* GENERATED_FIR_TAGS: funInterface, functionDeclaration, functionalType, integerLiteral, interfaceDeclaration,
lambdaLiteral, localProperty, propertyDeclaration, samConversion */
