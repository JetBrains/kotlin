// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

fun box() : String {
    var a = 1

    (object: Runnable {
        override public fun run() {
            a = 2
        }
    }).run()
    return if (a == 2) "OK" else "fail"
}
