// LANGUAGE: +ProperCheckAnnotationsTargetInTypeUsePositions
// ISSUE: KT-25876

@Target(AnnotationTarget.TYPE)
annotation class Anno(val value: String)

fun foo(x: String): @Anno(<!UNRESOLVED_REFERENCE!>Lorem<!>, <!TOO_MANY_ARGUMENTS!><!UNRESOLVED_REFERENCE!>ipsum<!>::class<!>, <!TOO_MANY_ARGUMENTS!>"dolor"<!>, <!TOO_MANY_ARGUMENTS!><!UNRESOLVED_REFERENCE!>sit<!><!DEBUG_INFO_MISSING_UNRESOLVED!>-<!><!UNRESOLVED_REFERENCE!>amet<!><!>) String {  // OK
    return x
}

abstract class Foo : @Anno(<!UNRESOLVED_REFERENCE!>o_O<!>) Throwable()  // OK

abstract class Bar<T : @Anno(<!UNRESOLVED_REFERENCE!>O_o<!>) Any>  // OK
