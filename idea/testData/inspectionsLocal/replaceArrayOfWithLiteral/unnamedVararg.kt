// LANGUAGE_VERSION: 1.2
// PROBLEM: none

annotation class Some(vararg val strings: String)

@Some(*<caret>arrayOf("alpha", "beta", "omega"))
class My
