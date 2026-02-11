// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -CONTEXT_RECEIVERS_DEPRECATED
// LANGUAGE: +ContextReceivers, -ContextParameters

data class Pair<A, B>(val first: A, val second: B)

context(Comparator<T>)
infix <!INAPPLICABLE_OPERATOR_MODIFIER!>operator<!> fun <T> T.compareTo(other: T) = <!UNRESOLVED_REFERENCE!>compare<!>(this, other)

context(Comparator<T>)
val <T> Pair<T, T>.max get() = if (first <!NO_CONTEXT_ARGUMENT!>><!> second) first else second

fun test() {
    val comparator = Comparator<String> { a, b ->
        if (a == null || b == null) 0 else a.length.compareTo(b.length)
    }
    with(comparator) {
        Pair("OK", "fail").max
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, comparisonExpression, data, disjunctionExpression, equalityExpression,
flexibleType, funWithExtensionReceiver, functionDeclaration, functionDeclarationWithContext, getter, ifExpression, infix,
integerLiteral, lambdaLiteral, localProperty, nullableType, operator, primaryConstructor, propertyDeclaration,
propertyDeclarationWithContext, propertyWithExtensionReceiver, smartcast, stringLiteral, thisExpression, typeParameter */
