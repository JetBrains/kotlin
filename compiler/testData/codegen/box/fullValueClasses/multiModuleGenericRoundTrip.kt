// LANGUAGE: +FullValueClasses
// WITH_STDLIB
// ISSUE: KT-84904
// DUMP_KLIB_ABI: DEFAULT

// MODULE: lib
// FILE: lib.kt

value class Entry<T>(val key: String?, val value: T)

fun makeEntry(): Entry<Int> = Entry("answer", 42)

// MODULE: main(lib)
// FILE: main.kt

fun box(): String {
    val entry = makeEntry()
    val equalEntry = Entry("answer", 42)

    if (entry.key != "answer") return "FAIL key"
    if (entry.value != 42) return "FAIL value"
    if (entry != equalEntry) return "FAIL equality"
    if (entry.toString() != equalEntry.toString()) return "FAIL toString"

    return "OK"
}
