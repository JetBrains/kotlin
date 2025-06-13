// RUN_PIPELINE_TILL: BACKEND
interface G {
    val a: Array<out G>
}

fun goo(g: G) {
    val x = g.a.firstOrNullX()
}

public fun <T> Array<out T>.firstOrNullX(): T? {
    return if (isEmpty()) null else this[0]
}

/* GENERATED_FIR_TAGS: capturedType, funWithExtensionReceiver, functionDeclaration, ifExpression, integerLiteral,
interfaceDeclaration, localProperty, nullableType, outProjection, propertyDeclaration, thisExpression, typeParameter */
