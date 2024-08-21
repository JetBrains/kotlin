// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt

import kotlin.<!UNRESOLVED_REFERENCE!>jvm<!>.<!DEBUG_INFO_MISSING_UNRESOLVED, DEPRECATION_ERROR{JVM}!>ImplicitlyActualizedByJvmDeclaration<!>

<!OPT_IN_WITHOUT_ARGUMENTS!>@OptIn(<!ANNOTATION_ARGUMENT_MUST_BE_CONST!><!UNRESOLVED_REFERENCE!>ExperimentalMultiplatform<!>::class<!>)<!>
@<!DEPRECATION_ERROR{JVM}, UNRESOLVED_REFERENCE!>ImplicitlyActualizedByJvmDeclaration<!>
expect class Foo() {
    fun foo()
}

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java

public class Foo {
    public void foo() {
    }
}
