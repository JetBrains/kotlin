// IGNORE_BACKEND: JVM_IR
// WITH_RUNTIME
// FILE: test.kt
fun test() {
    val result = Result.success("yes!")
    println("$result")
}

// @TestKt.class:
// 0 INVOKESTATIC kotlin/Result.box-impl
// 0 INVOKEVIRTUAL kotlin/Result.unbox-impl
