// RUN_PIPELINE_TILL: FRONTEND
fun foo(arg: Int?): Int {
    var i = arg
    if (i != null && <!DEBUG_INFO_SMARTCAST!>i<!>++ == 5) {
        return <!DEBUG_INFO_SMARTCAST!><!DEBUG_INFO_SMARTCAST!>i<!>--<!> + <!DEBUG_INFO_SMARTCAST!>i<!>
    }
    return 0
}

operator fun Long?.inc() = this?.let { it + 1 }

fun bar(arg: Long?): Long {
    var i = arg
    if (i++ == 5L) {
        return i<!UNSAFE_CALL!>--<!> <!DEBUG_INFO_MISSING_UNRESOLVED!>+<!> i
    }
    if (i++ == 7L) {
        return i++ <!UNSAFE_OPERATOR_CALL!>+<!> <!TYPE_MISMATCH!>i<!>
    }
    return 0L
}

/* GENERATED_FIR_TAGS: additiveExpression, andExpression, assignment, equalityExpression, funWithExtensionReceiver,
functionDeclaration, ifExpression, incrementDecrementExpression, integerLiteral, lambdaLiteral, localProperty,
nullableType, operator, propertyDeclaration, safeCall, smartcast, thisExpression */
