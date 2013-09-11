annotation class A1
annotation class A2(val some: Int = 12)

fun <<!UNSUPPORTED!>A1<!> <!UNSUPPORTED!>A2(3)<!> <!UNSUPPORTED!>A2<!> <!UNSUPPORTED!>A1(12)<!> <!UNSUPPORTED!>A2("Test")<!>  T> topFun() = 12

class SomeClass {
    fun <<!UNSUPPORTED!>A1<!> <!UNSUPPORTED!>A2(3)<!> <!UNSUPPORTED!>A2<!> <!UNSUPPORTED!>A1(12)<!> <!UNSUPPORTED!>A2("Test")<!> T> method() = 12

    fun foo() {
        fun <<!UNSUPPORTED!>A1<!> <!UNSUPPORTED!>A2(3)<!> <!UNSUPPORTED!>A2<!> <!UNSUPPORTED!>A1(12)<!> <!UNSUPPORTED!>A2("Test")<!>  T> innerFun() = 12
    }
}