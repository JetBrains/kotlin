// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
interface Inv<I>

fun <S, T: S> Inv<T>.reduce2(): S = null!!

fun test(a: Inv<Int>): Int {
    val b = 1 + a.reduce2()
    return b
}

/* GENERATED_FIR_TAGS: additiveExpression, checkNotNullCall, funWithExtensionReceiver, functionDeclaration,
integerLiteral, interfaceDeclaration, localProperty, nullableType, propertyDeclaration, typeConstraint, typeParameter */
