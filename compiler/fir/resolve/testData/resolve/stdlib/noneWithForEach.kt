interface Diagnostic {
    val name: String
}

fun foo(conflicting: List<Diagnostic>) {
    val filtered = arrayListOf<Diagnostic>()
    conflicting.groupBy {
        it.name
    }.forEach {
        val diagnostics = <!UNRESOLVED_REFERENCE!>it<!>.<!UNRESOLVED_REFERENCE!>value<!>
        filtered.addAll(
            diagnostics.<!AMBIGUITY!>filter<!> { me ->
                diagnostics.<!AMBIGUITY!>none<!> { other ->
                    me != other
                }
            }
        )
    }
}