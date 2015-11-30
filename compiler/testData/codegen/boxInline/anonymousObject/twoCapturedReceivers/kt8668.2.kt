package test

class A {

    fun callK(): String {
        return "K"
    }

    fun callO(): String {
        return "O"
    }

    fun testCall(): String = test { callO() }

    inline fun test(crossinline l: () -> String): String {
        return {
            l() + callK()
        }()
    }
}