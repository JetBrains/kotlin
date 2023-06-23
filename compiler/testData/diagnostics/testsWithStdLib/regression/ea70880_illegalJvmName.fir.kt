@JvmName<!NO_VALUE_FOR_PARAMETER!>()<!>
fun foo() {}

@JvmName(<!ARGUMENT_TYPE_MISMATCH!>42<!>)
fun bar() {}

@JvmName("a", <!TOO_MANY_ARGUMENTS!>"b"<!>)
fun baz() {}
