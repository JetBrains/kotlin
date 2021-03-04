@JvmName(<!NO_VALUE_FOR_PARAMETER!>)<!>
fun foo() {}

<!INAPPLICABLE_CANDIDATE!>@JvmName(42)<!>
fun bar() {}

@JvmName("a", <!TOO_MANY_ARGUMENTS!>"b"<!>)
fun baz() {}
