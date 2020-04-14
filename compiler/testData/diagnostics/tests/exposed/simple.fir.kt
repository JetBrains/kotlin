private interface My

private open class Base

public interface Your: My {
    fun <T: Base> foo(): T
}

public class Derived<T: My>(<!EXPOSED_PARAMETER_TYPE, EXPOSED_PROPERTY_TYPE!>val x: My<!>): Base() {

    constructor(<!EXPOSED_PARAMETER_TYPE!>xx: My?<!>, <!EXPOSED_PARAMETER_TYPE!>x: My<!>): this(xx ?: x)

    <!EXPOSED_PROPERTY_TYPE!>val y: Base? = null<!>

    val <!EXPOSED_RECEIVER_TYPE!>My<!>.z: Int
        get() = 42

    <!EXPOSED_FUNCTION_RETURN_TYPE!>fun foo(<!EXPOSED_PARAMETER_TYPE!>m: My<!>): My = m<!>

    <!EXPOSED_FUNCTION_RETURN_TYPE!>fun <!EXPOSED_RECEIVER_TYPE!>My<!>.bar(): My = this<!>
}


