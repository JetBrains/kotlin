// !WITH_NEW_INFERENCE
class Data<T>(val x: T, val y: T)

operator fun <T> Data<T>.component1() = x

operator fun <T> Data<T>.component2() = y

fun foo(): Int {
    val d: Data<Int>? = null
    // An error must be here
    val (x, y) = <!COMPONENT_FUNCTION_ON_NULLABLE, COMPONENT_FUNCTION_ON_NULLABLE!>d<!>
    return <!NI;TYPE_MISMATCH, NI;TYPE_MISMATCH!><!NI;DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!> <!NI;DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>+<!> <!NI;DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>y<!><!>
}

data class NormalData<T>(val x: T, val y: T)

fun bar(): Int {
    val d: NormalData<Int>? = null
    // An error must be here
    val (x, y) = <!NI;COMPONENT_FUNCTION_MISSING, NI;COMPONENT_FUNCTION_MISSING, OI;COMPONENT_FUNCTION_ON_NULLABLE, OI;COMPONENT_FUNCTION_ON_NULLABLE!>d<!>
    return <!NI;TYPE_MISMATCH, NI;TYPE_MISMATCH!><!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>x<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>+<!> <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>y<!><!>
}
