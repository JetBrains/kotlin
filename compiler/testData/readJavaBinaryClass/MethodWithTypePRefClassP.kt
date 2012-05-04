package test

open class MethodWithTypePRefClassP<erased P>() : java.lang.Object() {
    fun <erased Q : P> f() = #()
}
