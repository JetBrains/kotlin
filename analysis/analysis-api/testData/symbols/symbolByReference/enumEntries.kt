// LANGUAGE: +EnumEntries
// WITH_STDLIB

enum class MyKotlinEnum {
    FirstEntry, SecondEntry;
}

fun test() {
    MyKotlinEnum.entr<caret>ies
}
