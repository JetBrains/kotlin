// !WITH_NEW_INFERENCE
class Data<T>(val x: T, val y: T)

operator fun <T> Data<T>.component1() = x

operator fun <T> Data<T>.component2() = y

fun foo(): Int {
    val d: Data<Int>? = null
    // An error must be here
    val (x, y) = <!COMPONENT_FUNCTION_ON_NULLABLE, COMPONENT_FUNCTION_ON_NULLABLE!>d<!>
    return <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE{NI}!>x<!> <!DEBUG_INFO_MISSING_UNRESOLVED{NI}!>+<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE{NI}!>y<!>
}

data class NormalData<T>(val x: T, val y: T)

fun bar(): Int {
    val d: NormalData<Int>? = null
    // An error must be here
    val (x, y) = <!COMPONENT_FUNCTION_ON_NULLABLE, COMPONENT_FUNCTION_ON_NULLABLE!>d<!>
    return <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE{OI}, DEBUG_INFO_MISSING_UNRESOLVED{NI}!>+<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>y<!>
}
