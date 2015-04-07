// "Change 'bar' function return type to 'HashSet<Int>'" "true"

import java.util.HashSet

fun bar(): HashSet<Int> = java.util.LinkedHashSet<Int>()
fun foo(): java.util.HashSet<Int> = bar()