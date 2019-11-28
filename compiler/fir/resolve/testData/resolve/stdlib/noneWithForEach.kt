interface Diagnostic {
    val name: String
}

fun foo(conflicting: List<Diagnostic>) {
    val filtered = arrayListOf<Diagnostic>()
    conflicting.groupBy {
        it.name
    }.forEach {
        val diagnostics = it.value
        filtered.addAll(
            diagnostics.filter { me ->
                diagnostics.none { other ->
                    me != other
                }
            }
        )
    }
}