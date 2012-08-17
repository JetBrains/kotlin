class A {
    fun component1() : Int = 1
    fun component2() : Int = 2
}

fun a(<!UNUSED_PARAMETER!>aa<!> : A) {
    val (a: String, b1: String) = <!COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH, COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH!>aa<!>
}