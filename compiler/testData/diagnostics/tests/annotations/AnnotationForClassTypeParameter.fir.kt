annotation class A1
annotation class A2(val some: Int = 12)

class TopLevelClass<@A1 @A2(3) @A2 @A1(<!TOO_MANY_ARGUMENTS!>12<!>) @A2(<!ARGUMENT_TYPE_MISMATCH!>"Test"<!>) T> {
    class InnerClass<@A1 @A2(3) @A2 @A1(<!TOO_MANY_ARGUMENTS!>12<!>) @A2(<!ARGUMENT_TYPE_MISMATCH!>"Test"<!>) T> {
        fun test() {
            class InFun<@A1 @A2(3) @A2 @A1(<!TOO_MANY_ARGUMENTS!>12<!>) @A2(<!ARGUMENT_TYPE_MISMATCH!>"Test"<!>) T>
        }
    }
}
