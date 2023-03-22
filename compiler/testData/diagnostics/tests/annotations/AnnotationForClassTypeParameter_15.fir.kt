// IGNORE_REVERSED_RESOLVE
// !LANGUAGE: -ClassTypeParameterAnnotations
@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class A1

@Target(AnnotationTarget.TYPE_PARAMETER)
annotation class A2(val some: Int = 12)

class TopLevelClass<@A1 @A2(3) <!REPEATED_ANNOTATION!>@A2<!> <!REPEATED_ANNOTATION!>@A1(<!TOO_MANY_ARGUMENTS!>12<!>)<!> <!REPEATED_ANNOTATION!>@A2(<!ARGUMENT_TYPE_MISMATCH!>"Test"<!>)<!> T> {
    class InnerClass<@A1 @A2(3) <!REPEATED_ANNOTATION!>@A2<!> <!REPEATED_ANNOTATION!>@A1(<!TOO_MANY_ARGUMENTS!>12<!>)<!> <!REPEATED_ANNOTATION!>@A2(<!ARGUMENT_TYPE_MISMATCH!>"Test"<!>)<!> T> {
        fun test() {
            class InFun<@A1 @A2(3) <!REPEATED_ANNOTATION!>@A2<!> <!REPEATED_ANNOTATION!>@A1(<!TOO_MANY_ARGUMENTS!>12<!>)<!> <!REPEATED_ANNOTATION!>@A2(<!ARGUMENT_TYPE_MISMATCH!>"Test"<!>)<!> T>
        }
    }
}
