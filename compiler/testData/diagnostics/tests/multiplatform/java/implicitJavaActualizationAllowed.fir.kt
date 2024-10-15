// IGNORE_FIR_DIAGNOSTICS
// RUN_PIPELINE_TILL: FIR2IR
// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt

import <!DEPRECATION_ERROR!>kotlin.jvm.ImplicitlyActualizedByJvmDeclaration<!>

@OptIn(ExperimentalMultiplatform::class)
@<!DEPRECATION_ERROR!>ImplicitlyActualizedByJvmDeclaration<!>
<!NO_ACTUAL_FOR_EXPECT{JVM}!>expect<!> class Foo() {
    fun foo()
}

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java

public class Foo {
    public void foo() {
    }
}
