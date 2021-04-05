// !LANGUAGE: +ProhibitTypeParametersForLocalVariables

annotation class A1
annotation class A2(val some: Int = 12)

val <@A1 @A2(3) @A2 @A1(<!TOO_MANY_ARGUMENTS!>12<!>) @A2(<!ARGUMENT_TYPE_MISMATCH!>"Test"<!>)  T> T.topProp: Int get() = 12

class SomeClass {
    val <@A1 @A2(3) @A2 @A1(<!TOO_MANY_ARGUMENTS!>12<!>) @A2(<!ARGUMENT_TYPE_MISMATCH!>"Test"<!>) T> T.field: Int get() = 12

    fun foo() {
        val <@A1 @A2(3) @A2 @A1(12) @A2("Test") T> localVal = 12
    }
}
