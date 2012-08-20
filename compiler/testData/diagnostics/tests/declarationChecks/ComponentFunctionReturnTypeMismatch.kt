class A {
    fun component1() : Int = 1
    fun component2() : Int = 2
}

fun a(aa : A) {
    val (<!UNUSED_VARIABLE!>a<!>: String, <!UNUSED_VARIABLE!>b1<!>: String) = <!COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH, COMPONENT_FUNCTION_RETURN_TYPE_MISMATCH!>aa<!>
}