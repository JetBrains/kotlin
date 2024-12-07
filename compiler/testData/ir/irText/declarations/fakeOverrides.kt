// FIR_IDENTICAL
interface IFooStr {
    fun foo(x: String)
}

interface IBar {
    val bar: Int
}

abstract class CFoo<T> {
    fun foo(x: T) {}
}

class Test1 : CFoo<String>(), IFooStr, IBar {
    override val bar: Int = 42
}