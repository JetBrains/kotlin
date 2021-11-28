// WITH_STDLIB
// FILE: test.kt
fun test() {
    val result = Result.success("yes!")
    val failure = Result.failure<String>(Exception())

    if (result.isSuccess) println("success")
    if (result.isFailure) println("failure")
    println(result.getOrThrow())
    println(failure.getOrNull())
    println(failure.exceptionOrNull())

    println(result.hashCode())
    println(result.toString())

    println(result.getOrElse { "oops" })
    println(result.getOrDefault("oops"))
}

// @TestKt.class:
// 0 INVOKESTATIC kotlin/Result.box-impl
// 0 INVOKEVIRTUAL kotlin/Result.unbox-impl
// 0 Result\$Failure