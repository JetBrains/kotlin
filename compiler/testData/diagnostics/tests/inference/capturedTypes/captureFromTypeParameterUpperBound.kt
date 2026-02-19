// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
interface Inv<T>

fun <Y: X, X : Inv<out String>> foo(x: X, y: Y) {
    val rX = bar(x)
    rX.length

    val rY = bar(y)
    rY.length
}

fun <Y> bar(l: Inv<Y>): Y = TODO()

/* GENERATED_FIR_TAGS: capturedType, functionDeclaration, interfaceDeclaration, localProperty, nullableType,
outProjection, propertyDeclaration, typeConstraint, typeParameter */
