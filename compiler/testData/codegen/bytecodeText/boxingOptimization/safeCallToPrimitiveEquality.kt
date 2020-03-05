// IGNORE_BACKEND: JVM_IR
// TODO KT-36646 Don't box primitive values in equality comparison with nullable primitive values in JVM_IR

fun Long.id() = this

fun String.drop2() = if (length >= 2) subSequence(2, length) else null

fun doSimple1(s: String?) = s?.length == 3

fun doLongReceiver1(x: Long) = x?.id() == 3L

fun doChain1(s: String?) = s?.drop2()?.length == 1

fun doIf1(s: String?) =
        if (s?.length == 1) "A" else "B"

fun doSimple2(s: String?) = 3 == s?.length

fun doLongReceiver2(x: Long) = 3L == x?.id()

fun doChain2(s: String?) = 1 == s?.drop2()?.length

fun doIf2(s: String?) =
        if (1 == s?.length) "A" else "B"

// 0 valueOf
