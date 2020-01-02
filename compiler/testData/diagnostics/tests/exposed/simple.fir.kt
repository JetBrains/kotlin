private interface My

private open class Base

public interface Your: My {
    fun <T: Base> foo(): T
}

public class Derived<T: My>(val x: My): Base() {

    constructor(xx: My?, x: My): this(xx ?: x)

    val y: Base? = null

    val My.z: Int
        get() = 42

    fun foo(m: My): My = m

    fun My.bar(): My = this
}


