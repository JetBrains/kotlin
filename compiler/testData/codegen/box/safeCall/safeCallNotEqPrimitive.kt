// IGNORE_BACKEND_FIR: JVM_IR
fun Long.id() = this

fun String.drop2() = if (length >= 2) subSequence(2, length) else null

fun String.anyLength(): Any = length


fun doSimple(s: String?) = s?.length != 3

fun doLongReceiver(x: Long) = x?.id() != 3L

fun doChain(s: String?) = s?.drop2()?.length != 1

fun doIf(s: String?) =
        if (s?.length != 1) "A" else "B"

fun doCmpWithAny(s: String?) =
        s?.anyLength() != 3


fun box(): String = when {
    !doSimple(null) -> "failed 1"
    !doSimple("1") -> "failed 2"
    doSimple("123") -> "failed 3"

    !doLongReceiver(2L) -> "failed 4"
    doLongReceiver(3L) -> "failed 5"

    !doChain(null) -> "failed 6"
    !doChain("1") -> "failed 7"
    doChain("123") -> "failed 7"

    doIf("1") == "A" -> "failed 8"
    doIf("123") == "B" -> "failed 9"
    doIf(null) == "B" -> "failed 10"

    !doCmpWithAny(null) -> "failed 11"
    !doCmpWithAny("1") -> "failed 12"
    doCmpWithAny("123") -> "failed 13"


    else -> "OK"
}