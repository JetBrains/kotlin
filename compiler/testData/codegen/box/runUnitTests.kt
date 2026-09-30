// RUN_UNIT_TESTS
// KJS_WITH_FULL_RUNTIME
// IGNORE_BACKEND: JVM
// DISABLE_NATIVE
// ^^^ FirNativeCodegenBoxTestGenerated passes. It will be dropped in scope of KT-84713
// ^^^ KT-84713: NativeCodegenBoxTestGenerated fails with:
//       Expected exactly one executed testcase in the batch mode, but 5 outcomes were reported for [...]

// A box test driven through `kotlin.test`: every Native, JS, Wasm-js, WASI VM, including the standalone WasmEdge and Wasmtime,
// must run the `@Test` functions through the compiler's `startUnitTests` export.
// `kotlin.test` runs the suites of a file, and the tests of a class, in declaration order.

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

private val executedTests = mutableSetOf<String>()
private var boxCalls = 0

/** Every recording `@Test` function in this file; a name missing from [executedTests] is a test that did not run. */
private val expectedTests = setOf(
    "UnitTests.arithmetic",
    "UnitTests.strings",
    "MoreUnitTests.collections",
)

private fun missingTests(): List<String> = (expectedTests - executedTests).sorted()

class UnitTests {
    @Test
    fun arithmetic() {
        executedTests += "UnitTests.arithmetic"
        assertEquals(4, 2 + 2)
    }

    @Test
    fun strings() {
        executedTests += "UnitTests.strings"
        val greeting = buildString {
            append("Hello")
            append(", ")
            append("World")
        }
        assertTrue(greeting.startsWith("Hello"), greeting)
        assertEquals("Hello, World", greeting)
    }
}

class MoreUnitTests {
    @Test
    fun collections() {
        executedTests += "MoreUnitTests.collections"
        assertEquals(listOf(1, 4, 9), listOf(1, 2, 3).map { it * it })
    }
}

class AllTestsExecuted {
    @Test
    fun everyTestFunctionRan() {
        val missing = missingTests()
        assertTrue(missing.isEmpty(), "@Test functions not executed: $missing; executed: ${executedTests.sorted()}")
    }
}

fun box(): String {
    if (++boxCalls == 1) return "OK"
    val missing = missingTests()
    if (missing.isNotEmpty()) {
        return "FAIL: @Test functions not executed before box() call #$boxCalls: $missing; executed: ${executedTests.sorted()}"
    }
    return "OK"
}
