// LANGUAGE_VERSION: 1.2

annotation class Some(val strings: Array<String>)

@Some(<caret>arrayOf("alpha", "beta", "omega"))
class My
