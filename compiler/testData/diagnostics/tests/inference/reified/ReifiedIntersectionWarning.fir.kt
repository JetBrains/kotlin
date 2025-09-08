// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-52469
// LANGUAGE: -ProhibitIntersectionReifiedTypeParameter

interface InterfaceA
interface InterfaceB

inline fun <reified T> reifiedType(value: T) {}
inline fun <reified T> reifiedTypes(vararg values: T) {}

fun <T> testTypeParamter(value: T) {
    when (value) {
        is InterfaceA -> <!TYPE_INTERSECTION_AS_REIFIED_WARNING!>reifiedType<!>(value)
        is InterfaceB -> <!TYPE_INTERSECTION_AS_REIFIED_WARNING!>reifiedType<!>(value)
        is String -> <!TYPE_INTERSECTION_AS_REIFIED_WARNING!>reifiedType<!>(value)
        is Int -> <!TYPE_INTERSECTION_AS_REIFIED_WARNING!>reifiedType<!>(value)
        is List<*> -> <!TYPE_INTERSECTION_AS_REIFIED_WARNING!>reifiedType<!>(value)
        else -> <!TYPE_PARAMETER_AS_REIFIED!>reifiedType<!>(value)
    }
}

fun testInterfaces(value: InterfaceA) {
    when (value) {
        is InterfaceB -> <!TYPE_INTERSECTION_AS_REIFIED_WARNING!>reifiedType<!>(value)
        <!IMPOSSIBLE_IS_CHECK_ERROR!>is String<!> -> <!INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION, TYPE_INTERSECTION_AS_REIFIED_WARNING!>reifiedType<!>(value)
        <!IMPOSSIBLE_IS_CHECK_ERROR!>is Int<!> -> <!INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION, TYPE_INTERSECTION_AS_REIFIED_WARNING!>reifiedType<!>(value)
        is List<*> -> <!TYPE_INTERSECTION_AS_REIFIED_WARNING!>reifiedType<!>(value)
        else -> reifiedType(value)
    }
}

fun testArray(value: InterfaceA) {
    when (value) {
        is InterfaceB -> <!INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION, TYPE_INTERSECTION_AS_REIFIED_WARNING!>Array<!>(1) { _ -> value }
        <!IMPOSSIBLE_IS_CHECK_ERROR!>is String<!> -> <!TYPE_INTERSECTION_AS_REIFIED_WARNING!>Array<!>(1) { _ -> value }
        <!IMPOSSIBLE_IS_CHECK_ERROR!>is Int<!> -> <!TYPE_INTERSECTION_AS_REIFIED_WARNING!>Array<!>(1) { _ -> value }
        is List<*> -> <!TYPE_INTERSECTION_AS_REIFIED_WARNING!>Array<!>(1) { _ -> value }
        else -> Array(1) { _ -> value }
    }
}

fun testParameters() {
    <!TYPE_INTERSECTION_AS_REIFIED_WARNING!>reifiedTypes<!>(1, "2", false)
    reifiedTypes<Any>(1, "2", false)
}

/* GENERATED_FIR_TAGS: functionDeclaration, inline, integerLiteral, interfaceDeclaration, intersectionType, isExpression,
lambdaLiteral, nullableType, reified, smartcast, starProjection, stringLiteral, typeParameter, vararg, whenExpression,
whenWithSubject */
