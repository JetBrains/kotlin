
fun ff(l: Any) = when(l) {
    is MutableList<String> -> 1
    else <!SYNTAX!>2<!>
}
