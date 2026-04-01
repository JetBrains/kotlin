// NO_CHECK_LAMBDA_INLINING
// FILE: lib.kt
package foo
import kotlin.test.*

// CHECK_FUNCTION_EXISTS: runNoinline
// CHECK_NOT_CALLED: runNoinline

internal inline fun multiply(a: Int, b: Int) = a * b

internal inline fun run(a: Int, b: Int, func: (Int, Int) -> Int) = func(a, b)

internal inline fun runNoinline(a: Int, b: Int, noinline func: (Int, Int) -> Int) = func(a, b)

// FILE: main.kt
package foo
import kotlin.test.*

internal fun multiplyInline(a: Int, b: Int) = run(a, b, ::multiply)

// CHECK_FUNCTION_EXISTS: runNoinline
// CHECK_NOT_CALLED: runNoinline

internal fun multiplyNoinline(a: Int, b: Int) = runNoinline(a, b, ::multiply)


fun box(): String {
    assertEquals(6, multiplyInline(2, 3))
    assertEquals(6, multiplyNoinline(2, 3))

    return "OK"
}