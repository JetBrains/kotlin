interface <caret>I {
    // INFO: {"checked": "true", "toAbstract": "true"}
    val x: Int get() = 2
    // INFO: {"checked": "true", "toAbstract": "true"}
    val y: Int
    // INFO: {"checked": "true", "toAbstract": "true"}
    fun foo(n: Int): Boolean = n > 0
    // INFO: {"checked": "true", "toAbstract": "true"}
    fun bar(s: String)

    // INFO: {"checked": "true", "toAbstract": "true"}
    class Y {

    }
}

abstract class A : I

class B : I {
    override val y = 1

    override fun bar(s: String) = s.length()
}

interface J : I

interface K : I {
    override val y: Int get() = 1

    override fun bar(s: String) = s.length()
}