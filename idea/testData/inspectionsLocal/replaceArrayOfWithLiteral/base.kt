// ERROR: The feature "array literals in annotations" is only available since language version 1.2

annotation class Some(val strings: Array<String>)

@Some(strings = <caret>arrayOf("alpha", "beta", "omega"))
class My
