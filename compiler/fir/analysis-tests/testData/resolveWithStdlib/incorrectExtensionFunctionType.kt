// ISSUE: KT-58446

infix fun Int.foo(x: <!WRONG_EXTENSION_FUNCTION_TYPE!>@ExtensionFunctionType<!> Int) {}

fun bar() {
    1 foo fun<!SYNTAX!><!>
}
