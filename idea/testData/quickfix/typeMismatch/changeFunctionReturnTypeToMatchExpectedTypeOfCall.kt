// "Change 'bar' function return type to 'String'" "true"
fun bar(): Any = ""
fun foo(): String = bar(<caret>)