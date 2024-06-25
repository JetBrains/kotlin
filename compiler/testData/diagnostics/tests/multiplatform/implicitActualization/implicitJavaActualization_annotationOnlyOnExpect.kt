// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt

import kotlin.jvm.ImplicitlyActualizedByJvmDeclaration

annotation class Annot

@OptIn(ExperimentalMultiplatform::class)
@ImplicitlyActualizedByJvmDeclaration
@Annot
expect class Foo

// MODULE: m2-jvm()()(m1-common)
// FILE: Foo.java
public class Foo {}
