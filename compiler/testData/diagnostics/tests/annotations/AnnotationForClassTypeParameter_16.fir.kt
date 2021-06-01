// !LANGUAGE: +ClassTypeParameterAnnotations
annotation class A1
annotation class A2(val some: Int = 12)

@Target(AnnotationTarget.TYPE)
annotation class TA1

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class TA2(val some: Int = 12)

class TopLevelClass<@A1 @A2(3) @A2 @A1(<!TOO_MANY_ARGUMENTS!>12<!>) @A2(<!ARGUMENT_TYPE_MISMATCH!>"Test"<!>) T> {
    class InnerClass<@A1 @A2(3) @A2 @A1(<!TOO_MANY_ARGUMENTS!>12<!>) @A2(<!ARGUMENT_TYPE_MISMATCH!>"Test"<!>) T> {
        fun test() {
            class InFun<@A1 @A2(3) @A2 @A1(<!TOO_MANY_ARGUMENTS!>12<!>) @A2(<!ARGUMENT_TYPE_MISMATCH!>"Test"<!>) T>
        }
    }
}

class TTopLevelClass<@TA1 @TA2(3) @TA2 @TA1(<!TOO_MANY_ARGUMENTS!>12<!>) @TA2(<!ARGUMENT_TYPE_MISMATCH!>"Test"<!>) T> {
    class TInnerClass<@TA1 @TA2(3) @TA2 @TA1(<!TOO_MANY_ARGUMENTS!>12<!>) @TA2(<!ARGUMENT_TYPE_MISMATCH!>"Test"<!>) T> {
        fun test() {
            class TInFun<@TA1 @TA2(3) @TA2 @TA1(<!TOO_MANY_ARGUMENTS!>12<!>) @TA2(<!ARGUMENT_TYPE_MISMATCH!>"Test"<!>) T>
        }
    }
}
