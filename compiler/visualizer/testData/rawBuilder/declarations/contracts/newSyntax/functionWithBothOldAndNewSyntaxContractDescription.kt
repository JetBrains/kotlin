// FIR_IGNORE
fun test1(s: String?) contract [returnsNotNull()] {
//  [ERROR: not resolved]
//  │
    contract {
//      [ERROR: not resolved]
//      │         [ERROR: not resolved]
//      │         │        test1.s: String?
//      │         │        │ EQ operator call
//      │         │        │ │  Nothing?
//      │         │        │ │  │
        returns() implies (s != null)
    }
//  fun test1(String?): Unit
//  │
    test1()
}
