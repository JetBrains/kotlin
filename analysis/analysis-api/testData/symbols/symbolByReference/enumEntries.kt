// DO_NOT_CHECK_SYMBOL_RESTORE_K1
// !LANGUAGE: +EnumEntries
// WITH_STDLIB

enum class MyKotlinEnum {
    FirstEntry, SecondEntry;
}

fun test() {
    MyKotlinEnum.entr<caret>ies
}
