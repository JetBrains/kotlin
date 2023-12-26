// WITH_STDLIB
// JVM_ABI_K1_K2_DIFF: KT-63864

import kotlin.test.*

val log = StringBuilder()

fun logged(message: String, value: Int) =
    value.also { log.append(message) }

fun box(): String {
    var sum = 0
    for (i in logged("start;", 1) until logged("end;", 5)) {
        sum = sum * 10 + i
    }

    assertEquals(1234, sum)

    assertEquals("start;end;", log.toString())

    return "OK"
}