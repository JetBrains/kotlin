open class A {
    private val number = 4
        <!EXPOSING_GETTERS_UNSUPPORTED!>public<!> get(): <!WRONG_GETTER_RETURN_TYPE!>Number<!>
}
