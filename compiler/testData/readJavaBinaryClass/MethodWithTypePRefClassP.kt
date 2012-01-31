package test

open class MethodWithTypePRefClassP<erased P>() {
    fun <erased Q : P> f() = #()
}
