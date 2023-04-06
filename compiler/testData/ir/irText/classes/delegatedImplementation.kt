// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57754

interface IBase {
    fun foo(x: Int, s: String)
    fun bar(): Int
    fun String.qux()
}

object BaseImpl : IBase {
    override fun foo(x: Int, s: String) {}
    override fun bar(): Int = 42
    override fun String.qux() {}
}

interface IOther {
    val x: String
    var y: Int
    val Byte.z1: Int
    var Byte.z2: Int
}

fun otherImpl(x0: String, y0: Int): IOther = object : IOther {
    override val x: String = x0
    override var y: Int = y0
    override val Byte.z1: Int get() = 1
    override var Byte.z2: Int
        get() = 2
        set(value) {}
}

class Test1 : IBase by BaseImpl

class Test2 : IBase by BaseImpl, IOther by otherImpl("", 42)
