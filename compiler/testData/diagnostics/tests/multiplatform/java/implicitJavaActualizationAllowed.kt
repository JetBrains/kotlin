// WITH_STDLIB
// DONT_STOP_ON_FIR_ERRORS

// MODULE: m1-common
// FILE: common.kt

import kotlin.jvm.ImplicitlyActualizedByJvmDeclaration

@OptIn(ExperimentalMultiplatform::class)
@ImplicitlyActualizedByJvmDeclaration
expect class Foo() {
    fun foo()
}

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java

public class Foo {
    public void foo() {
    }
}
