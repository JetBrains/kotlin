val <T> T.foo : T?
    get() = null

fun test(): Int? {
    return 1.<!EXPLICIT_TYPE_ARGUMENTS_IN_PROPERTY_ACCESS!>foo<!>
}