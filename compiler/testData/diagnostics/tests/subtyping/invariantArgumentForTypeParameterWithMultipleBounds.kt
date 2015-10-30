import java.io.Serializable

class A<T> where T : Cloneable, T : Serializable

interface CS1 : Cloneable, Serializable
interface CS2 : CS1

interface I1 {
    fun foo(): A<in CS2>
}

interface I2 : I1 {
    override fun foo(): A<CS1>
}

interface I3 : I1 {
    override fun foo(): A<in CS1>
}
