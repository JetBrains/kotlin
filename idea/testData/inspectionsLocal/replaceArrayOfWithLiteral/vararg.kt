// LANGUAGE_VERSION: 1.2

annotation class Some(vararg val strings: String)

@Some(strings = *<caret>arrayOf("alpha", "beta", "omega"))
class My
