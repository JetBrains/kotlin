// IGNORE_DIAGNOSTIC_API
// IGNORE_REVERSED_RESOLVE
// Ignore reason: KT-58786

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