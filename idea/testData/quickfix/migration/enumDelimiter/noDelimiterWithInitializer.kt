// "Insert lacking comma(s) / semicolon(s)" "true"

enum class MyEnum(val z: Int) {
    A: MyEnum(3)
    B<caret>: MyEnum(7)
    C: MyEnum(12)
    fun foo() = z * 2
}