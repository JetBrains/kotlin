fun Long.id() = this

fun Short.id() = this

fun String.drop2() = if (length >= 2) subSequence(2, length) else null

fun doSimple1(s: String?) = s?.length == 3

fun doLongReceiver1(x: Long?) = x?.id() == 3L

fun doShortReceiver1(x: Short?, y: Short) = x?.id() == y

fun doIf1(s: String?) =
        if (s?.length == 1) "A" else "B"

fun doSimple2(s: String?) = 3 == s?.length

fun doLongReceiver2(x: Long?) = 3L == x?.id()

fun doShortReceiver2(x: Short?, y: Short) = y == x?.id()

fun doIf2(s: String?) =
        if (1 == s?.length) "A" else "B"

// 0 valueOf
