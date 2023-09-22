import java.io.Serializable

interface Test1 {
    fun <T> foo(t: T) where T : Cloneable, T : Serializable
    @Deprecated("foo", level = DeprecationLevel.HIDDEN)
    fun <T> foo(t: T) where T : Serializable, T : Cloneable
}


interface I1
interface I2 : I1

interface Test2 {
    fun <T> foo(t: T) where T : I1, T : I2
    @Deprecated("foo", level = DeprecationLevel.HIDDEN)
    fun <T> foo(t: T) where T : I2, T : I1
}
