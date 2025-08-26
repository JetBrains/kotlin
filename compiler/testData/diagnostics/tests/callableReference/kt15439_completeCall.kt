// RUN_PIPELINE_TILL: BACKEND
// FIR_IDENTICAL
// DIAGNOSTICS: -UNUSED_VARIABLE, -UNUSED_VARIABLE

fun test() {
    data class Pair<F, S>(val first: F, val second: S)
    val (first, second) =
            Pair(1,
                 if (1 == 1)
                     Pair<String, String>::first
                 else
                     Pair<String, String>::second)
}

/* GENERATED_FIR_TAGS: callableReference, classDeclaration, data, destructuringDeclaration, equalityExpression,
functionDeclaration, ifExpression, integerLiteral, localClass, localProperty, nullableType, primaryConstructor,
propertyDeclaration, typeParameter */
