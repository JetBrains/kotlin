// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS

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
