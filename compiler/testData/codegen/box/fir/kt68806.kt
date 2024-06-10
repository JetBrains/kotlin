// WITH_STDLIB
// DUMP_IR
// ISSUE: KT-68806

fun parse(a: Int): List<Int> {
    val x = Any()
    return sequence {
        when(a) {
            0 -> {
                if (a == 0) x.hashCode()
            }
            else -> when(a) {
                0 -> {
                    yield(1)
                }
                else -> {
                    x.hashCode()
                }
            }
        }
    }.toList()
}

fun box(): String {
    parse(1)
    return "OK"
}