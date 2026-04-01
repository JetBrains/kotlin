// DO_NOT_REQUIRE_SYMBOL_RESTORATION_K1
// LANGUAGE: +EnumEntries
// WITH_STDLIB

enum class MyKotlinEnum {
    FirstEntry, SecondEntry;
}

fun test() {
    MyKotlinEnum.entr<caret>ies
}
