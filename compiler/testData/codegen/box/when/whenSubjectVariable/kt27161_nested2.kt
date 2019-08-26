// IGNORE_BACKEND: WASM
// IGNORE_BACKEND_FIR: JVM_IR
enum class Test {
    A, B, OTHER
}

fun peek() = Test.A

fun box(): String {
    val x = when (val t1 = peek()) {
        Test.A -> {
            when (
                val t2 = when(val y = peek()) {
                    Test.A -> Test.A
                    Test.B -> Test.B
                    else -> Test.OTHER
                }
            ) {
                Test.A ->
                    when (val t3 = peek()) {
                        Test.A -> "OK"
                        else -> "other 3"
                    }

                else -> "other 2"
            }
        }

        else -> "other 1"
    }
    return x
}

// DONT_TARGET_EXACT_BACKEND: WASM
 //DONT_TARGET_WASM_REASON: ENUMS
