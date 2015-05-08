// "Insert lacking comma(s) / semicolon(s)" "true"

enum class MyEnum {
    A {
        override fun foo(): Int = 13
    }
    B C<caret> {
        override fun foo(): Int = 23
    }
    open fun foo(): Int = 42
}