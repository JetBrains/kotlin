// !DIAGNOSTICS: -UNUSED_PARAMETER -UNUSED_VARIABLE


fun a() {
    val x = 10
    <!INAPPLICABLE_CANDIDATE!>foo<!>(::<!UNSUPPORTED!>x<!>)
}

fun foo(a: Any) {}

fun test1(test2: () -> Unit = ::test2) {} // Resolve to function
private fun test2() {}
fun test3(test4: () -> Unit = ::<!UNSUPPORTED!>test4<!>) {}

fun test5(test6: (test: Test) -> Unit = Test::helper) {
    test6(Test())
}

class Test {
    fun helper() {}
}

