// RUN_PIPELINE_TILL: FRONTEND
// DISABLE_NEXT_TIER_SUGGESTION: Disabling EnumEntries is not normal operation mode
// LANGUAGE: -EnumEntries
// WITH_STDLIB
// ISSUE: KT-55251

enum class Foo {
    BAR;
}

fun main() {
    Foo.entries
}
