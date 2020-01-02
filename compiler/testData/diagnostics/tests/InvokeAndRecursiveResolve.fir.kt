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
    val bar = test()
    val test = <!UNRESOLVED_REFERENCE!>bar<!>()
}