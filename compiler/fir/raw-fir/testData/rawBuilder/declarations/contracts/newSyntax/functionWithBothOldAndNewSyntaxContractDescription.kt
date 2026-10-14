fun test1(s: String?) contract [returnsNotNull()] {
    contract {
        returns() implies (s != null)
    }
    test1()
}