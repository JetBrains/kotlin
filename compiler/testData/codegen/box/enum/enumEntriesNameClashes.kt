// !LANGUAGE: +EnumEntries
// IGNORE_BACKEND_FIR: JVM_IR
// IGNORE_BACKEND: JS, JVM
// WITH_STDLIB

enum class EnumWithClash {
    values,
    entries,
    valueOf;
}

@OptIn(ExperimentalStdlibApi::class)
fun box(): String {
    val ref = EnumWithClash::entries
    if (ref().toString() != "[values, entries, valueOf]") return "FAIL 1"
    if (EnumWithClash.entries.toString() != "entries") return "FAIL 2"
    return "OK"
}