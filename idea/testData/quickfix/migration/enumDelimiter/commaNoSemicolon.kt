// "Insert lacking comma(s) / semicolon(s)" "true"

enum class MyEnum {
    FIRST, 
    SECOND<caret>,
    val zzz = 42
}