// "Change return type of called function 'bar' to 'HashSet<Int>'" "true"

fun bar(): Any = java.util.LinkedHashSet<Int>()
fun foo(): java.util.HashSet<Int> = bar(<caret>)