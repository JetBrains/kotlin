// DONT_TARGET_EXACT_BACKEND: JS_IR, JS_IR_ES6, WASM_JS, WASM_WASI
// ^ returnType is not supported

// WITH_REFLECT

import kotlin.reflect.KVariance
import kotlin.test.assertEquals

class Fourple<A, B, C, D>
fun foo(): Fourple<String, in String, out String, *> = null!!

fun listOfStrings(): List<String> = null!!

fun box(): String {
    assertEquals(
            listOf(
                    KVariance.INVARIANT,
                    KVariance.IN,
                    KVariance.OUT,
                    null
            ),
            ::foo.returnType.arguments.map { it.variance }
    )

    // Declaration-site variance should have no effect on the variance of the type projection:
    assertEquals(KVariance.INVARIANT, ::listOfStrings.returnType.arguments.first().variance)

    return "OK"
}
