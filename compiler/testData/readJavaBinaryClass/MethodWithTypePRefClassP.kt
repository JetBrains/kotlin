package test

open class MethodWithTypePRefClassP<P>() : java.lang.Object() {
    fun <Q : P> f() = #()
}
