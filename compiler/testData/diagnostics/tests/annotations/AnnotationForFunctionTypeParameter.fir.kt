annotation class A1
annotation class A2(val some: Int = 12)

fun <@A1 @A2(3) @A2 @A1(<!TOO_MANY_ARGUMENTS!>12<!>) @A2(<!ARGUMENT_TYPE_MISMATCH!>"Test"<!>)  T> topFun() = 12

class SomeClass {
    fun <@A1 @A2(3) @A2 @A1(<!TOO_MANY_ARGUMENTS!>12<!>) @A2(<!ARGUMENT_TYPE_MISMATCH!>"Test"<!>) T> method() = 12

    fun foo() {
        fun <@A1 @A2(3) @A2 @A1(<!TOO_MANY_ARGUMENTS!>12<!>) @A2(<!ARGUMENT_TYPE_MISMATCH!>"Test"<!>)  T> innerFun() = 12
    }
}
