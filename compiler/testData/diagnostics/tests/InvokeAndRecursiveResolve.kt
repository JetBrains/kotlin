// !WITH_NEW_INFERENCE
// NI_EXPECTED_FILE

fun test() = 3

fun <T> proxy(t: T) = t

class A {
    val test = test()
}

class B {
    val test = proxy(test())
}

class C {
    val bar = <!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!><!UNINITIALIZED_VARIABLE!>test<!>()<!>
    val test = <!NI;TYPECHECKER_HAS_RUN_INTO_RECURSIVE_PROBLEM!><!NI;DEBUG_INFO_MISSING_UNRESOLVED, OI;FUNCTION_EXPECTED!>bar<!>()<!>
}