// FIR_IDENTICAL
// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-74999
// FIR_DUMP

interface Traversable {
    fun foo(): String = "fail"
}
interface Entity<S : Entity<S>> : Traversable {
    fun <T : S> isEqualTo(): T
}

interface Path : Traversable

fun <K> select(x: K, y: K): K = x

fun foo(path: Path, e: Entity<*>): String {
    // Tv <: Capture(*)
    // Tv <: Kv
    // Path <: Kv
    // Kv := Path
    // Tv <: Capture(*) & Path
    // Tv <: Path
    return select(e.isEqualTo(), path).foo()
}
