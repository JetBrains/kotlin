// WITH_STDLIB
// JVM_ABI_K1_K2_DIFF: KT-63864

import kotlin.test.*

val log = StringBuilder()

fun logged(message: String, value: Int) =
    value.also { log.append(message) }

fun box(): String {
    var sum = 0
    for (i in ((logged("start;", 9) downTo logged("end;", 0) step logged("step2;", 2)).reversed() step logged("step3;", 3)).reversed()) {
        sum = sum * 10 + i
    }

    assertEquals(741, sum)

    assertEquals("start;end;step2;step3;", log.toString())

    return "OK"
}