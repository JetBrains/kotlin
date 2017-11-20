// !WITH_NEW_INFERENCE
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
    val test = <!FUNCTION_EXPECTED!>bar<!>()
}