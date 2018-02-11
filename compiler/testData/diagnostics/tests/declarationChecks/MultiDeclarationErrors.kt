// !WITH_NEW_INFERENCE
package a

class MyClass {
    fun component1(<!UNUSED_PARAMETER!>i<!>: Int) {}
}

class MyClass2 {}

<!CONFLICTING_OVERLOADS!>fun MyClass2.component1()<!> = 1.2
<!CONFLICTING_OVERLOADS!>fun MyClass2.component1()<!> = 1.3

fun test(mc1: MyClass, mc2: MyClass2) {
    val (<!OPERATOR_MODIFIER_REQUIRED!>a<!>, b) = <!COMPONENT_FUNCTION_MISSING, COMPONENT_FUNCTION_MISSING!>mc1<!>
    val (c) = <!COMPONENT_FUNCTION_AMBIGUITY!>mc2<!>

    //a,b,c are error types
    use(<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>a<!>, <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>b<!>, <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>c<!>)
}

fun use(vararg a: Any?) = a