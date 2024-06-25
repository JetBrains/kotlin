// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt

import kotlin.jvm.ImplicitlyActualizedByJvmDeclaration

@OptIn(ExperimentalMultiplatform::class)
@ImplicitlyActualizedByJvmDeclaration
expect class <!NO_ACTUAL_FOR_EXPECT{JVM}!>Foo<!>()

// MODULE: m2-jvm()()(m1-common)
