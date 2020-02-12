// WITH_RUNTIME
// IGNORE_BACKEND: JVM_IR
// TODO KT-36648 Captured variables not optimized in JVM_IR

// In JVM IR, SharedVariablesLowering transforms `x` into a shared variable to be able to update it from a lambda,
// which is a separate function (...$lambda-0).
// If we keep the existing representation of lambda bodies as separate functions in JVM IR, the only viable option to fix this test
// seems to support this case in the bytecode optimization pass CapturedVarsOptimizationMethodTransformer.

fun box(): String {
    val x: String
    run {
        x = "OK"
        val y = x
    }
    return x
}

// 0 ObjectRef
