// "Insert lacking comma(s) / semicolon(s)" "true"

enum class MyEnum {
    FIRST SECOND,
    THIRD
    FOURTH<caret> FIFTH SIXTH,
    SEVENTH EIGHTH

}