// WITH_RUNTIME
open class A

abstract class <caret>B: A() {
    // INFO: {"checked": "true", "toAbstract": "true"}
    val x = 1
    // INFO: {"checked": "true", "toAbstract": "true"}
    val y: Int get() = 2
    // INFO: {"checked": "true", "toAbstract": "true"}
    val z: Int by lazy { 3 }
    // INFO: {"checked": "true", "toAbstract": "true"}
    abstract val t: Int
    // INFO: {"checked": "true", "toAbstract": "true"}
    fun foo(n: Int): Boolean = n > 0
    // INFO: {"checked": "true", "toAbstract": "true"}
    abstract fun bar(s: String)

    // INFO: {"checked": "true", "toAbstract": "true"}
    inner class X {

    }

    // INFO: {"checked": "true", "toAbstract": "true"}
    class Y {

    }
}