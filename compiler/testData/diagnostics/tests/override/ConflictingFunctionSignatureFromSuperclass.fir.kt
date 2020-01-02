open class Aaa() {
    fun foo() = 1
}

open class Bbb() : Aaa() {
    fun <T> foo() = 2
}
