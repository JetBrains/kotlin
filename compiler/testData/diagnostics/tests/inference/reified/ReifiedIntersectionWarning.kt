// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-52469
// LANGUAGE: -ProhibitIntersectionReifiedTypeParameter

interface InterfaceA
interface InterfaceB

inline fun <reified T> reifiedType(value: T) {}
inline fun <reified T> reifiedTypes(vararg values: T) {}

fun <T> testTypeParamter(value: T) {
    when (value) {
        is InterfaceA -> reifiedType(<!DEBUG_INFO_SMARTCAST!>value<!>)
        is InterfaceB -> reifiedType(<!DEBUG_INFO_SMARTCAST!>value<!>)
        is String -> reifiedType(<!DEBUG_INFO_SMARTCAST!>value<!>)
        is Int -> reifiedType(<!DEBUG_INFO_SMARTCAST!>value<!>)
        is List<*> -> reifiedType(<!DEBUG_INFO_SMARTCAST!>value<!>)
        else -> <!TYPE_PARAMETER_AS_REIFIED!>reifiedType<!>(value)
    }
}

fun testInterfaces(value: InterfaceA) {
    when (value) {
        is InterfaceB -> reifiedType(value)
        is <!INCOMPATIBLE_TYPES!>String<!> -> <!INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION!>reifiedType<!>(value)
        is <!INCOMPATIBLE_TYPES!>Int<!> -> <!INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION!>reifiedType<!>(value)
        is List<*> -> reifiedType(value)
        else -> reifiedType(value)
    }
}

fun testArray(value: InterfaceA) {
    <!INFERRED_TYPE_VARIABLE_INTO_POSSIBLE_EMPTY_INTERSECTION!>when (value) {
        is InterfaceB -> Array(1) { _ -> value }
        is <!INCOMPATIBLE_TYPES!>String<!> -> Array(1) { _ -> value }
        is <!INCOMPATIBLE_TYPES!>Int<!> -> Array(1) { _ -> value }
        is List<*> -> Array(1) { _ -> value }
        else -> Array(1) { _ -> value }
    }<!>
}

fun testParameters() {
    reifiedTypes(1, "2", false)
    reifiedTypes<Any>(1, "2", false)
}
