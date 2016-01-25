// From KT-10753
object My : <!EXPOSED_SUPER_CLASS!>Inter()<!> {
    fun <!EXPOSED_FUNCTION_RETURN_TYPE!>foo<!>(<!EXPOSED_PARAMETER_TYPE!>arg: Inter<!>): Inter = arg
    <!EXPOSED_PROPERTY_TYPE!>val x: Inter? = null<!>
}

internal open class Inter

// From KT-10799
open class Test {
    protected class Protected

    fun <!EXPOSED_FUNCTION_RETURN_TYPE!>foo<!>(<!EXPOSED_PARAMETER_TYPE!>x: Protected<!>) = x

    interface NestedInterface {
        fun create(<!EXPOSED_PARAMETER_TYPE!>x: Protected<!>)
    }

    class NestedClass {
        fun <!EXPOSED_FUNCTION_RETURN_TYPE!>create<!>(<!EXPOSED_PARAMETER_TYPE!>x: Protected<!>) = x
    }

    object NestedObject {
        fun <!EXPOSED_FUNCTION_RETURN_TYPE!>create<!>(<!EXPOSED_PARAMETER_TYPE!>x: Protected<!>) = x
    }

    companion object {
        fun <!EXPOSED_FUNCTION_RETURN_TYPE!>create<!>(<!EXPOSED_PARAMETER_TYPE!>x: Protected<!>) = x
    }
}
