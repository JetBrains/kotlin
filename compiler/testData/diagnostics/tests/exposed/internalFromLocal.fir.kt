interface Your

class My {
    internal val x = object : Your {}

    internal fun <!EXPOSED_FUNCTION_RETURN_TYPE!>foo<!>() = {
        class Local
        Local()
    }()
}