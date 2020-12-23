val foo by <!UNRESOLVED_REFERENCE!>foo<!>

val bar by <!UNRESOLVED_REFERENCE!><!INAPPLICABLE_CANDIDATE!>baz<!>(bar)<!>

fun <T> baz(t: T): T = t
