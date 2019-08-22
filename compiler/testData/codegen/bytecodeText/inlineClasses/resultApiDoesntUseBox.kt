// IGNORE_BACKEND: JVM
// IGNORE_BACKEND: JVM_IR
// WITH_COROUTINES
// FILE: test.kt
fun test() {
    val result = Result.success("yes!")
    val failure = Result.failure<String>(Exception())

    if (result.isSuccess) println("success")
    if (result.isFailure) println("failure")
    println(result.getOrThrow())
    println(failure.getOrNull())
    println(failure.exceptionOrNull())

    val other = Result.success("nope")
    if (result == other) println("==")
    if (result != other) println("!=")
    if (result.equals(other)) println("equals") // Boxes (JVM, JVM_IR)
    if (!result.equals(other)) println("!equals") // Boxes (JVM, JVM_IR)

    println(result.hashCode())
    println(result.toString())
    println("$result") // Boxes (JVM_IR)

    val ans1 = runCatching { 42 }
    println(ans1) // Boxes (JVM, JVM_IR)

    val ans2 = 42.runCatching { this }
    println(ans2) // Boxes (JVM, JVM_IR)

    println(result.getOrElse { "oops" })
    println(result.getOrDefault("oops"))
}

// @TestKt.class:
// 0 INVOKESTATIC kotlin/Result.box-impl
// 0 INVOKEVIRTUAL kotlin/Result.unbox-impl
// 0 Result\$Failure