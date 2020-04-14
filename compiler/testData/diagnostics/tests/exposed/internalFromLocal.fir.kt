interface Your

class My {
    internal val x = object : Your {}

    <!EXPOSED_FUNCTION_RETURN_TYPE!>internal fun foo() = {
        class Local
        Local()
    }()<!>
}