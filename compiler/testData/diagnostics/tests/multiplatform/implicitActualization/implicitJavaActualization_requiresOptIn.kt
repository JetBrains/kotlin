// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt

import kotlin.jvm.ImplicitlyActualizedByJvmDeclaration

@OptIn(ExperimentalMultiplatform::class)
@ImplicitlyActualizedByJvmDeclaration
expect annotation class Foo

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java
@kotlin.RequiresOptIn
public @interface Foo {}
