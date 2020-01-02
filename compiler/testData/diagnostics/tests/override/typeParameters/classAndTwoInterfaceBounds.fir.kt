interface I1
interface I2
open class C

interface A {
    fun <T> foo(t: T) where T : I1, T : C, T : I2
}

interface B1 : A {
    override fun <T> foo(t: T) where T : C, T : I1, T : I2
}

interface B2 : A {
    override fun <T> foo(t: T) where T : I1, T : C, T : I2
}

interface B3 : A {
    override fun <T> foo(t: T) where T : I1, T : I2, T : C
}

interface B4 : A {
    override fun <T> foo(t: T) where T : C, T : I2, T : I1
}

interface B5 : A {
    override fun <T> foo(t: T) where T : I2, T : C, T : I1
}

interface B6 : A {
    override fun <T> foo(t: T) where T : I2, T : I1, T : C
}
