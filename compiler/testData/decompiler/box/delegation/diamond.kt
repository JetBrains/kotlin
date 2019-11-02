interface Base {
    fun getValue(): String
    fun test(): String = getValue()
}

public interface Base2 : Base {
    override fun test(): String = "O" + getValue()
}

interface KBase : Base

interface Derived : KBase, Base2

class Fail : Derived {
    override fun getValue() = "Fail"
}

fun box(): String {
    val z = object : Derived by Fail() {
        override fun getValue() = "K"
    }
    return z.test()
}
