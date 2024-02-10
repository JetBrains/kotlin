// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER
<!ILLEGAL_JVM_NAME!>@JvmName("")<!>
fun foo(a: Any) {}

<!ILLEGAL_JVM_NAME!>@JvmName(".")<!>
fun foo() {}

<!ILLEGAL_JVM_NAME!>@JvmName("/")<!>
fun fooSlash() {}

<!ILLEGAL_JVM_NAME!>@JvmName("<")<!>
fun fooLT() {}

class Foo {
    @JvmName("getFoo")
    <!NON_FINAL_MEMBER_IN_FINAL_CLASS!>open<!> fun foo() {}
}
