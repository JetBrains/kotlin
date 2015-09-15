package test

annotation class Ann(val s1: String)

@Ann(s1 = """a""" + "b") class MyClass

// EXPECTED: @Ann(s1 = "ab")
