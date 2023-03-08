// FIR_IDENTICAL
// JVM_TARGET: 1.8
// WITH_STDLIB

// MODULE: lib
// FILE: tests.kt

interface BaseTest {
    fun getProject() = Any()
}

open class GradleTestCase {
    @get:JvmName("myProject")
    val project = Any()
}

open class GradleCodeInsightTestCase: GradleTestCase(), BaseTest

// MODULE: main(lib)
// FILE: main.kt

class GradleActionTest: GradleCodeInsightTestCase() // K1: ok, K2: CONFLICTING_INHERITED_JVM_DECLARATIONS
