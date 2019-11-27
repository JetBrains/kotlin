// IGNORE_BACKEND_FIR: JVM_IR
fun Long.id() = this

fun String.drop2() = if (length >= 2) subSequence(2, length) else null

fun String.anyLength(): Any = length


fun doSimple(s: String?) = 3 == s?.length

fun doLongReceiver(x: Long) = 3L == x?.id()

fun doChain(s: String?) = 1 == s?.drop2()?.length

fun doIf(s: String?) =
        if (1 == s?.length) "A" else "B"

fun doCmpWithAny(s: String?) =
        3 == s?.anyLength()

fun doIfNot(s: String?) =
        if (!(1 == s?.length)) "A" else "B"

fun doIfNotNot(s: String?) =
        if (!!(1 == s?.length)) "A" else "B"


fun box(): String = when {
    doSimple(null) -> "failed 1"
    doSimple("1") -> "failed 2"
    !doSimple("123") -> "failed 3"

    doLongReceiver(2L) -> "failed 4"
    !doLongReceiver(3L) -> "failed 5"

    doChain(null) -> "failed 6"
    doChain("1") -> "failed 7"
    !doChain("123") -> "failed 7"

    doIf("1") != "A" -> "failed 8"
    doIf("123") != "B" -> "failed 9"
    doIf(null) != "B" -> "failed 10"

    doCmpWithAny(null) -> "failed 11"
    doCmpWithAny("1") -> "failed 12"
    !doCmpWithAny("123") -> "failed 13"

    doIfNot("1") != "B" -> "failed 8"
    doIfNot("123") != "A" -> "failed 9"
    doIfNot(null) != "A" -> "failed 10"

    doIfNotNot("1") != "A" -> "failed 8"
    doIfNotNot("123") != "B" -> "failed 9"
    doIfNotNot(null) != "B" -> "failed 10"

    else -> "OK"
}