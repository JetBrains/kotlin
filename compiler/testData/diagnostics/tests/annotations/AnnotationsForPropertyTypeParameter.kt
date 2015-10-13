annotation class A1
annotation class A2(val some: Int = 12)

val <<!UNSUPPORTED!>@A1<!> <!UNSUPPORTED!>@A2(3)<!> <!UNSUPPORTED!>@A2<!> <!UNSUPPORTED!>@A1(12)<!> <!UNSUPPORTED!>@A2("Test")<!>  T> T.topProp: Int get() = 12

class SomeClass {
    val <<!UNSUPPORTED!>@A1<!> <!UNSUPPORTED!>@A2(3)<!> <!UNSUPPORTED!>@A2<!> <!UNSUPPORTED!>@A1(12)<!> <!UNSUPPORTED!>@A2("Test")<!> T> T.field: Int get() = 12

    fun foo() {
        val <<!UNSUPPORTED!>@A1<!> <!UNSUPPORTED!>@A2(3)<!> <!UNSUPPORTED!>@A2<!> <!UNSUPPORTED!>@A1(12)<!> <!UNSUPPORTED!>@A2("Test")<!> T> <!UNUSED_VARIABLE!>localVal<!> = 12
    }
}