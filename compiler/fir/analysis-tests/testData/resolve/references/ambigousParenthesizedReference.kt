// ISSUE: KT-36958

fun consume(x: Any?) {}

fun box() = consume((Int::<!OVERLOAD_RESOLUTION_AMBIGUITY!>plus<!>))
