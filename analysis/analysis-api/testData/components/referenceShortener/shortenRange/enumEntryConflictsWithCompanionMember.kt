enum class MyEnum {
    m;
    companion object { val m = 1 }
    fun context() = <expr>MyEnum.Companion.m</expr>
}
