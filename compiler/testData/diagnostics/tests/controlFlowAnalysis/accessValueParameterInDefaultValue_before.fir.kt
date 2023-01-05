// LANGUAGE: -ProhibitIllegalValueParameterUsageInDefaultArguments
// DIAGNOSTICS: -UNUSED_PARAMETER
// WITH_STDLIB
// ISSUE: KT-25694

fun test_1(
    x: () -> String = { <!UNINITIALIZED_PARAMETER!>y<!> }, // Error
    y: String = "OK"
) {}

fun test_2(
    x: String = "OK",
    y: () -> String = { x } // OK
) {}

fun test_3(
    x: () -> Any = { <!UNINITIALIZED_PARAMETER!>y<!>() to <!UNINITIALIZED_PARAMETER!>y<!>.invoke() }, // Error
    y: () -> String = { "OK" }
) {}

fun test_4(
    x: () -> String = { "OK" },
    y: () -> Any = { x() to x.invoke() } // OK
) {}

interface Foo {
    fun foo()
}

fun test_5(
    x: Foo = object : Foo {
        val z1 = <!UNINITIALIZED_PARAMETER!>y<!> // Error
        val z2 = run { <!UNINITIALIZED_PARAMETER!>y<!> } // Error

        init {
            println(<!UNINITIALIZED_PARAMETER!>y<!>) // Error
        }

        override fun foo() {
            println(<!UNINITIALIZED_PARAMETER!>y<!>) // Error
        }
    },
    y: String = "OK"
) {}

fun test_6(
    x: String = "OK",
    y: Foo = object : Foo {
        val z1 = x // OK
        val z2 = run { x } // OK

        init {
            println(x) // OK
        }

        override fun foo() {
            println(x) // OK
        }
    }
) {}

fun getFoo(x: String): Foo = null!!

fun test_7(
    x: Foo = object : Foo by getFoo(<!UNINITIALIZED_PARAMETER!>y<!>) {}, // Error
    y: String = "OK"
) {}

fun test_8(
    x: String = "OK",
    y: Foo = object : Foo by getFoo(x) {} // OK
) {}
