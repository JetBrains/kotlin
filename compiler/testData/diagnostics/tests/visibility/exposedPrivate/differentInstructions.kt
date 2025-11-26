// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -NOTHING_TO_INLINE -UNUSED_EXPRESSION
// WITH_STDLIB

private class C : Throwable() {
    companion object {
        @JvmField
        var staticField = Any()
    }
}

private inline fun ldc() { C::class }
private inline fun <reified T> ldcReified() { T::class }
private inline fun newarray() { emptyArray<C>() }
private inline fun checkcast(x: Any?) { x as C? }
private inline fun instanceof(x: Any?) { x is C? }
private inline fun getstatic() {
    C.staticField
}
private inline fun putstatic() {
    C.staticField = Any()
}
private inline fun tryCatch() {
    try {} catch (c: C) {}
}

internal inline fun test() {
    ldc()
    ldcReified<C>()
    newarray()
    checkcast(null)
    instanceof(null)
    getstatic()
    putstatic()
    tryCatch()
}

/* GENERATED_FIR_TAGS: asExpression, assignment, classDeclaration, classReference, companionObject, functionDeclaration,
inline, isExpression, localProperty, nullableType, objectDeclaration, propertyDeclaration, reified, tryExpression,
typeParameter */
