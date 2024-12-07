// FIR_IDENTICAL

val test1 = "\uD83E\uDD17"
val test2 = "\uD83E\uDD17\uD83E\uDD17"

const val testConst1 = "\uD83E\uDD17"
const val testConst2 = "\uD83E\uDD17\uD83E\uDD17"
const val testConst3 = "\uD83E\uDD17$testConst2"
const val testConst4 = "$testConst2$testConst2"

fun test1(x: Int) = "\uD83E\uDD17$x"

fun test2(x: Int) = "$x\uD83E\uDD17"

fun test3(x: Int) = "$x\uD83E\uDD17$x"
