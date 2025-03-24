// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// WITH_STDLIB

fun foo(arg: Boolean) = buildString {
    append("Alpha")
    if (arg) {
        append("Omega")
    }
}

interface Sam<S> {
    fun run(s: S): String
}

fun <O> bar(f: (O) -> String) = object : Sam<O> {
    override fun run(s: O): String = f(s)
}

var flag = true

val baz = bar { it: String? ->
    if (it.isNullOrBlank()) {
        buildString {
            append("Alpha")
            if (flag) {
                append("Omega")
            }
        }
    } else {
        ""
    }
}
