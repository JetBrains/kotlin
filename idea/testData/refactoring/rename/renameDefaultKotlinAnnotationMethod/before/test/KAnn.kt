package test

annotation class KAnn(val /*rename*/value: String)

@KAnn("abc")
fun test1() {}

@KAnn(value = "abc")
fun test2() {}