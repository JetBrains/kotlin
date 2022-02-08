// FIR_IDENTICAL
// !DIAGNOSTICS: -ASSIGNED_BUT_NEVER_ACCESSED_VARIABLE -UNCHECKED_CAST
// WITH_STDLIB

sealed interface Metric {}

class Counter: Metric
class Gauge<T>: Metric

fun <K> foo(y: List<K?>, x: Inv<in K?>, p: (Inv<K?>, K?) -> Unit) {}
fun <M> materialize(): M = null as M

class Inv<L>(var x: L)

fun <T : Metric?> register(name: String, metric: T): T? {
    when (metric) {
        is Counter -> {
            return metric
        }
        is Gauge<*> -> {
            foo(listOf(), Inv(metric)) { x, y ->
                var a = y
                a = materialize()
            }
            return metric
        }
        else -> return null
    }
}
