// IGNORE_BACKEND: JVM_IR
// FILE: C.kt
class CInt(val value: Int)
val nCInt3: CInt? = CInt(3)

class CLong(val value: Long)
val nCLong3: CLong? = CLong(3)

// FILE: test.kt
fun testInt(i: Int?) =
        when (i) {
            0 -> "zero"
            42 -> "magic"
            else -> "other"
        }

fun testLong(i: Long?) =
        when (i) {
            0L -> "zero"
            42L -> "magic"
            else -> "other"
        }

// @TestKt.class:
// 0 valueOf
// 0 Integer.valueOf
// 0 Long.valueOf
// 0 areEqual