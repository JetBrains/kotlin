// TARGET_BACKEND: JVM
// WITH_STDLIB
// FILE: box.kt
fun box(): String = A.result

object A {
    @JvmField
    val result: String

    init { result = Z.result }
}

// FILE: z.kt
object Z {
    @JvmField
    val result: String = "OK"
}
