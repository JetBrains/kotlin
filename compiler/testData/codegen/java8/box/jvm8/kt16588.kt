// JVM_TARGET: 1.8

fun number(doLong: Boolean): Number = when {
    doLong -> 1.toLong()
    else -> 0
}

fun box(): String {
    number(true)
    return "OK"
}

