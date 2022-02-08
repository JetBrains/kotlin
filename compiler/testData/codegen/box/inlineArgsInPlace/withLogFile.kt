// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK

fun condition1() = true

fun zzz() {}

fun f2() = 2

// Minimized version of
// https://github.com/JetBrains/kotlin/commit/ced973b7074f4207859d9709375f2bf28b3e2c55#diff-f9a8dce85985573b5478da1b5379342fe37fca94c14f55b69d1c884fece42f92R841
fun box(): String {
    val arr = arrayOfNulls<Int>(4)

    fun zap(threadNo: Int): String {
        arr[threadNo] = try {
            f2()
        } catch (e: Exception) {
            null
        }
        arr[threadNo] = when {
            condition1() -> {
                1
            }
            else -> {
                println("[$threadNo] 3")
                3
            }
        }
        return "OK"
    }

    return zap(0)
}