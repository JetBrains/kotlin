// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt

import <!DEPRECATION_ERROR!>kotlin.jvm.ImplicitlyActualizedByJvmDeclaration<!>

<!NO_ACTUAL_FOR_EXPECT{JVM}!>@OptIn(ExperimentalMultiplatform::class)
@<!DEPRECATION_ERROR!>ImplicitlyActualizedByJvmDeclaration<!>
expect class Foo() {
    fun foo()
}<!>

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java

public class Foo {
    public void foo() {
    }
}
