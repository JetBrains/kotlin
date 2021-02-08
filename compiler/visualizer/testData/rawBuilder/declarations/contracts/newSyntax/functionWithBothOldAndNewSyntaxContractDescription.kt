fun test1(s: String?) contract [returnsNotNull()] {
//  [ERROR: not resolved]
//  │
    contract {
//      [ERROR: not resolved]
//      │         [ERROR: not resolved]
//      │         │        test1.s: String?
//      │         │        │ fun (Any).equals(Any?): Boolean
//      │         │        │ │  Nothing?
//      │         │        │ │  │
        returns() implies (s != null)
    }
//  fun test1(String?): Unit
//  │
    test1()
}
