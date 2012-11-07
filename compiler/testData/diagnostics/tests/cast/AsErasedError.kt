
fun ff(c: MutableCollection<String>) = c <!CAST_NEVER_SUCCEEDS!>as<!> MutableList<Int>
