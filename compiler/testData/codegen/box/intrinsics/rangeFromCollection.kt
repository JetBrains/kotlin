// IGNORE_BACKEND_FIR: JVM_IR
// KJS_WITH_FULL_RUNTIME
fun box(): String {
    val list = ArrayList<IntRange>()
    list.add(1..3)
    list[0].start
    return "OK"
}
