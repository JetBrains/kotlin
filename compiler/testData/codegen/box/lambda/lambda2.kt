// WITH_STDLIB

import kotlin.test.*

val sb = StringBuilder()

fun box(): String {
    mainFun(arrayOf("OK"))
    return sb.toString()
}

fun mainFun(args : Array<String>) {
    run {
        sb.append(args[0])
    }
}

fun run(f: () -> Unit) {
    f()
}
