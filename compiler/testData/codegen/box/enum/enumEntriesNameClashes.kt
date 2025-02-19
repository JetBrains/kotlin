// LANGUAGE: +EnumEntries -PrioritizedEnumEntries -ForbidEnumEntryNamedEntries
// KT-59611
// WITH_STDLIB
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD
// ^^^ Compiler v2.1.10 does not know this language feature

import kotlin.enums.*

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
    if (enumEntries<EnumWithClash>().toString() != "[values, entries, valueOf]") return "FAIL 3"
    return "OK"
}
