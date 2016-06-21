package test

annotation class KAnn(val valueNew: String)

@KAnn(valueNew = "abc")
fun test1() {}

@KAnn(valueNew = "abc")
fun test2() {}