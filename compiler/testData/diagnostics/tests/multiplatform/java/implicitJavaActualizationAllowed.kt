// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt

import kotlin.jvm.<!DEPRECATION_ERROR{JVM}!>ImplicitlyActualizedByJvmDeclaration<!>

@OptIn(ExperimentalMultiplatform::class)
@<!DEPRECATION_ERROR{JVM}!>ImplicitlyActualizedByJvmDeclaration<!>
expect class Foo() {
    fun foo()
}

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java

public class Foo {
    public void foo() {
    }
}
