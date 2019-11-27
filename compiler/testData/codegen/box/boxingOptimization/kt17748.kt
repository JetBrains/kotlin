// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

fun box(): String {
    42.doSwitchInt()
    "".doSwitchString()
    return "OK"
}

inline fun <reified E> E.doSwitchInt(): String = when (E::class) {
    Int::class -> "success!"
    else -> throw AssertionError()
}

inline fun <reified E> E.doSwitchString(): String = when(E::class) {
    String::class -> "success!"
    else -> throw AssertionError()
}