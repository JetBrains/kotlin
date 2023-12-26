// WITH_STDLIB
// JVM_ABI_K1_K2_DIFF: KT-63864

import kotlin.test.*

val log = StringBuilder()

fun logged(message: String, value: Int) =
    value.also { log.append(message) }

fun box(): String {
    var sum = 0
    for (i in logged("start;", 4) downTo logged("end;", 1)) {
        sum = sum * 10 + i
    }

    assertEquals(4321, sum)

    assertEquals("start;end;", log.toString())

    return "OK"
}