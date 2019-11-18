// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

// FULL_JDK

fun box() : String {
    val processors = Runtime.getRuntime()!!.availableProcessors()
    var threadNum = 1
    while(threadNum <= 1024) {
        if(threadNum < 2 * processors)
            threadNum += 1
        else
            threadNum *= 2
    }
    return "OK"
}
