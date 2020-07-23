// !LANGUAGE: +ProhibitTypeParametersForLocalVariables

annotation class A1
annotation class A2(val some: Int = 12)

val <@A1 @A2(3) @A2 <!INAPPLICABLE_CANDIDATE!>@A1(12)<!> <!INAPPLICABLE_CANDIDATE!>@A2("Test")<!>  T> T.topProp: Int get() = 12

class SomeClass {
    val <@A1 @A2(3) @A2 <!INAPPLICABLE_CANDIDATE!>@A1(12)<!> <!INAPPLICABLE_CANDIDATE!>@A2("Test")<!> T> T.field: Int get() = 12

    fun foo() {
        val <@A1 @A2(3) @A2 @A1(12) @A2("Test") T> localVal = 12
    }
}
