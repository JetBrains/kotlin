const val BOOL = <!EVALUATED("true")!>true<!>
const val BOOL_OR = <!EVALUATED("false")!>false && BOOL<!>
const val BOOL_AND = <!EVALUATED("true")!>true || BOOL<!>
const val BOOL_AND_OR = <!EVALUATED("true")!>true || false && BOOL<!>

fun box() = "OK"
