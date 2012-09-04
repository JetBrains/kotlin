
fun ff(c: Collection<String>) = c <!CAST_NEVER_SUCCEEDS!>as<!> List<Int>
