// TARGET_BACKEND: JVM
// TARGET_BACKEND: JVM_IR

fun box() : String {
    var a = 1

    (object: Runnable {
        override public fun run() {
            a = 2
        }
    }).run()
    return if (a == 2) "OK" else "fail"
}
