// LANGUAGE: +EnumEntries -PrioritizedEnumEntries -ForbidEnumEntryNamedEntries
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_1_9
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_0
// ^^^ Compiler v2.0.0 does not know this language feature
// KT-59611
// WITH_STDLIB
// IGNORE_NATIVE: compatibilityTestMode=BACKWARD_2_1
// ^^^ Compiler v2.1.0 does not know this language feature

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
