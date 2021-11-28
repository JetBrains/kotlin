fun foo(x: Int) {}

fun bar() {
    foo(<!NAMED_PARAMETER_NOT_FOUND!>y<!> = 1<!NO_VALUE_FOR_PARAMETER!>)<!>
}
