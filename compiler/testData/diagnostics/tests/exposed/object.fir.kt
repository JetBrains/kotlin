// From KT-10753
object My : Inter() {
    <!EXPOSED_FUNCTION_RETURN_TYPE!>fun foo(<!EXPOSED_PARAMETER_TYPE!>arg: Inter<!>): Inter = arg<!>
    <!EXPOSED_PROPERTY_TYPE!>val x: Inter? = null<!>
}

internal open class Inter

// From KT-10799
open class Test {
    protected class Protected

    <!EXPOSED_FUNCTION_RETURN_TYPE!>fun foo(<!EXPOSED_PARAMETER_TYPE!>x: Protected<!>) = x<!>

    interface NestedInterface {
        fun create(<!EXPOSED_PARAMETER_TYPE!>x: Protected<!>)
    }

    class NestedClass {
        <!EXPOSED_FUNCTION_RETURN_TYPE!>fun create(<!EXPOSED_PARAMETER_TYPE!>x: Protected<!>) = x<!>
    }

    object NestedObject {
        <!EXPOSED_FUNCTION_RETURN_TYPE!>fun create(<!EXPOSED_PARAMETER_TYPE!>x: Protected<!>) = x<!>
    }

    companion object {
        <!EXPOSED_FUNCTION_RETURN_TYPE!>fun create(<!EXPOSED_PARAMETER_TYPE!>x: Protected<!>) = x<!>
    }
}
