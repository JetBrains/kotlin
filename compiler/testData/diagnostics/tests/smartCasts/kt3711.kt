// RUN_PIPELINE_TILL: BACKEND
data class StringPair(val first: String, val second: String)

infix fun String.to(second: String) = StringPair(this, second)

fun f(a: String?) {
    if (a != null) {
        val b: StringPair = <!DEBUG_INFO_SMARTCAST!>a<!> to <!DEBUG_INFO_SMARTCAST!>a<!>
    }
}

/* GENERATED_FIR_TAGS: classDeclaration, data, equalityExpression, funWithExtensionReceiver, functionDeclaration,
ifExpression, infix, localProperty, nullableType, primaryConstructor, propertyDeclaration, smartcast, thisExpression */
