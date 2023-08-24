// FIR_IDENTICAL
// !LANGUAGE: +EnumEntries +PrioritizedEnumEntries
// WITH_STDLIB
// FIR_DUMP
// ISSUE: KT-56587

enum class E05 {
    ;
    object entries
}

fun test05() {
    println(E05.entries)
}

enum class E07(val entries: String) {
    ;
    fun test() {
        println(entries)
    }
}

enum class E071 {
    ;
    constructor(entries: String) {
        println(entries)
    }
}

enum class E09 {
    ;
    val entries: String = "entries"
    fun test() {
        println(entries)
    }
}

interface I01 {
    val entries: String
        get() = "entries"
}

enum class E10 : I01 {
    ;
    fun test() {
        println(entries)
    }
}
