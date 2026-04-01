package test

enum class MyEnum {
    A, B;
    companion object {
        fun parse(): MyEnum = <expr>MyEnum.A</expr>
    }
}
