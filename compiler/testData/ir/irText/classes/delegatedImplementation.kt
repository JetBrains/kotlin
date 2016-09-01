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

interface IOther
fun otherImpl(): IOther = object : IOther {}

class Test1 : IBase by BaseImpl

class Test2 : IBase by BaseImpl, IOther by otherImpl()