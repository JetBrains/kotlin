// IGNORE_BACKEND_FIR: JVM_IR
// TARGET_BACKEND: JVM

fun box(): String {
    val a = HashSet<String>()
    a.add("live")
    a.add("long")
    a.add("prosper")
    val b = a.clone()
    if (a != b) return "Fail equals"
    if (a === b) return "Fail identity"
    return "OK"
}
