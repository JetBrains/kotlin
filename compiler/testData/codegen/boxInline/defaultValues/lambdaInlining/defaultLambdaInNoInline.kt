// IGNORE_BACKEND: JVM_IR
// FILE: 1.kt
// SKIP_INLINE_CHECK_IN: inlineFun$default
package test

inline fun inlineFun(crossinline inlineLambda: () -> String = { "OK" }, noinline noInlineLambda: () -> String = { inlineLambda() }): String {
    return noInlineLambda()
}

// FILE: 2.kt
//NO_CHECK_LAMBDA_INLINING
// CHECK_CALLED_IN_SCOPE: function=inlineFun$lambda_0 scope=box
import test.*

fun box(): String {
    return inlineFun()
}
