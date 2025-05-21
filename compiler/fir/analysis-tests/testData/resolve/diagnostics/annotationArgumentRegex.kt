// RUN_PIPELINE_TILL: FRONTEND
// ISSUE: KT-75833

annotation class Foo(val string: String)

@Foo("<!ILLEGAL_ESCAPE!>\d<!>")
class Bar
