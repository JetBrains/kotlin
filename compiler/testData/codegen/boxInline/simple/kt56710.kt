// ENABLE_JVM_IR_INLINER
// MODULE: lib
// FILE: 1.kt

fun funWithLambda(doSomething: () -> String): String = doSomething()

inline fun nestedFunWithLambda(): String = funWithLambda { "OK" }

// MODULE: main(lib)
// FILE: 2.kt
fun box(): String = nestedFunWithLambda()
