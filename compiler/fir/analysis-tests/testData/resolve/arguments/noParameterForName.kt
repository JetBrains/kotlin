fun foo(x: Int) {}

fun bar() {
    foo(<!NO_VALUE_FOR_PARAMETER!><!NAMED_PARAMETER_NOT_FOUND!>y<!> = 1)<!>
}
