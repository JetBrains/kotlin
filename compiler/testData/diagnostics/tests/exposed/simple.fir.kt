private interface My

private open class Base

public interface Your: <!EXPOSED_SUPER_INTERFACE!>My<!> {
    fun <T: Base> foo(): T
}

public class Derived<T: <!EXPOSED_TYPE_PARAMETER_BOUND!>My<!>>(<!EXPOSED_PARAMETER_TYPE!>val <!EXPOSED_PROPERTY_TYPE!>x<!>: My<!>): <!EXPOSED_SUPER_CLASS!>Base<!>() {

    constructor(<!EXPOSED_PARAMETER_TYPE!>xx: My?<!>, <!EXPOSED_PARAMETER_TYPE!>x: My<!>): this(xx ?: x)

    val <!EXPOSED_PROPERTY_TYPE!>y<!>: Base? = null

    val <!EXPOSED_RECEIVER_TYPE!>My<!>.z: Int
        get() = 42

    fun <!EXPOSED_FUNCTION_RETURN_TYPE!>foo<!>(<!EXPOSED_PARAMETER_TYPE!>m: My<!>): My = m

    fun <!EXPOSED_RECEIVER_TYPE!>My<!>.<!EXPOSED_FUNCTION_RETURN_TYPE!>bar<!>(): My = this
}


