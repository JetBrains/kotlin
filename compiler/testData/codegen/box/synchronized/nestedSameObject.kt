// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// WITH_RUNTIME

fun box(): String {
    val obj = "" as java.lang.Object

    synchronized (obj) {
        synchronized (obj) {
            obj.wait(1)
        }
    }

    return "OK"
}
