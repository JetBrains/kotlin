// WITH_RUNTIME
// DISABLE-ERRORS
interface T<X> {
    fun <caret>foo(x: X): X
}

class U : T<String> {

}

class V : T<Int> {

}

class Z : T<Int> by V() {

}

class W : T<Boolean> {
    override fun foo(x: Boolean): Boolean {
        throw UnsupportedOperationException()
    }
}