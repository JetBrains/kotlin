// ISSUE: KT-20223

class Test {
    internal operator fun invoke() = this
}

<!NOTHING_TO_INLINE!>inline<!> fun testFunction() = Test().<!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>invoke<!>()
<!NOTHING_TO_INLINE!>inline<!> fun testOperator() = <!NON_PUBLIC_CALL_FROM_PUBLIC_INLINE!>Test()<!>()
