<!ILLEGAL_JVM_NAME!>@JvmName(<!NO_VALUE_FOR_PARAMETER!>)<!><!>
fun foo() {}

<!ILLEGAL_JVM_NAME!>@JvmName(<!CONSTANT_EXPECTED_TYPE_MISMATCH!>42<!>)<!>
fun bar() {}

@JvmName("a", <!TOO_MANY_ARGUMENTS!>"b"<!>)
fun baz() {}
