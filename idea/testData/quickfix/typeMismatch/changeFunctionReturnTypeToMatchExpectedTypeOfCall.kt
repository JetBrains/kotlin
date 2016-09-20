// "Change return type of invoked function 'bar' to 'String'" "true"
fun bar(): Any = ""
fun foo(): String = bar(<caret>)