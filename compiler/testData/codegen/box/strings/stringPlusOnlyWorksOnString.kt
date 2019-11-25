// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
// WITH_RUNTIME

fun box(): String {
    var x: MutableCollection<Int> = ArrayList()
    x + ArrayList()
    return "OK"
}
