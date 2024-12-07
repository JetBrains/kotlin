// RUN_PIPELINE_TILL: BACKEND
// LANGUAGE: -ForbidInferringTypeVariablesIntoEmptyIntersection
// RENDER_DIAGNOSTICS_FULL_TEXT
open class Foo
inline fun <reified T : Foo> g(): T? = null

inline fun <R> f(block: ()->R?): R? {
    return block()
}

fun main() {
    f<Int> { <!INFERRED_TYPE_VARIABLE_INTO_EMPTY_INTERSECTION_WARNING, TYPE_INTERSECTION_AS_REIFIED_WARNING!>g<!>() }
}
