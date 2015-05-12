// "Insert lacking comma(s) / semicolon(s)" "true"

enum class MyEnum {
    A; B; C<caret>; D
    fun foo() = 42
}