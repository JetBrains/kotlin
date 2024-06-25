// WITH_STDLIB

// MODULE: m1-common
// FILE: common.kt

import kotlin.jvm.ImplicitlyActualizedByJvmDeclaration

<!NO_ACTUAL_FOR_EXPECT{JVM}!>@OptIn(ExperimentalMultiplatform::class)
@ImplicitlyActualizedByJvmDeclaration
expect class Foo {
    fun foo(a: Int)
}<!>

// MODULE: lib()()()
// FILE: lib.kt
class Foo {
    fun foo(a: Int = 1) {}
}

// MODULE: m2-jvm(lib)()(m1-common)
// FILE: jvm.kt
