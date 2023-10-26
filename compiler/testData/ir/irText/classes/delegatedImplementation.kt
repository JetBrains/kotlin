// FIR_IDENTICAL
interface IBase {
    fun foo(x: Int, s: String)
    fun foo(x: Int)
    fun foo(x: String)
    fun <T> foo(t: T)
    fun <S, T> foo(s: S, t: T)
    fun <T : IBase> foo(t: T)
    fun <T> foo(t: T) where T : IBase, T: IOther
    fun bar(): Int
    fun String.qux()
    fun String.qux(s: String)
    fun Int.foo()
}

object BaseImpl : IBase {
    override fun foo(x: Int, s: String) {}
    override fun foo(x: Int) {}
    override fun foo(x: String) {}
    override fun <T> foo(t: T) {}
    override fun <S, T> foo(s: S, t: T) {}
    override fun <T : IBase> foo(t: T) {}
    override fun <T> foo(t: T) where T : IBase, T: IOther {}
    override fun bar(): Int = 42
    override fun String.qux() {}
    override fun String.qux(s: String) {}
    override fun Int.foo() {}
}

interface IOther {
    val x: String
    var y: Int
    val Byte.z1: Int
    var Byte.z2: Int
    val <T> T.z3: Int
    val <T : IBase> T.z3: Int
    val <T> T.z3: Int where T : IBase, T: IOther
}

fun otherImpl(x0: String, y0: Int): IOther = object : IOther {
    override val x: String = x0
    override var y: Int = y0
    override val Byte.z1: Int get() = 1
    override var Byte.z2: Int
        get() = 2
        set(value) {}
    override val <T> T.z3: Int get() = 0
    override val <T : IBase> T.z3: Int get() = 0
    override val <T> T.z3: Int where T : IBase, T: IOther get() = 0
}

class Test1 : IBase by BaseImpl

class Test2 : IBase by BaseImpl, IOther by otherImpl("", 42)
