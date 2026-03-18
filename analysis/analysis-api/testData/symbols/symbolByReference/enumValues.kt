// DO_NOT_REQUIRE_SYMBOL_RESTORATION_K1

enum class MyKotlinEnum {
    FirstEntry, SecondEntry;
}

fun test() {
    MyKotlinEnum.valu<caret>es()
}