<!INAPPLICABLE_CANDIDATE!>@JvmName()<!>
fun foo() {}

@JvmName(<!ARGUMENT_TYPE_MISMATCH!>42<!>)
fun bar() {}

<!INAPPLICABLE_CANDIDATE!>@JvmName("a", "b")<!>
fun baz() {}
