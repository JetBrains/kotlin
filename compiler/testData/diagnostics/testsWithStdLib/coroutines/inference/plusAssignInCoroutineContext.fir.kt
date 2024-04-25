// DIAGNOSTICS: -UNCHECKED_CAST -OPT_IN_USAGE_ERROR -UNUSED_PARAMETER

class Bar

fun <T> materialize() = null as T

interface FlowCollector<in T> {
    suspend fun emit(value: T)
}

interface Flow<T>

public fun <T> flow(block: suspend FlowCollector<T>.() -> Unit) = materialize<Flow<T>>()

fun foo(total: Int, next: Int) = 10
fun foo(total: Int, next: Float) = 10
fun foo(total: Float, next: Int) = 10

fun call(x: String) {}

suspend fun foo(x: Int) = flow {
    var y = 1
    y += if (x > 2) 1 else 2

    var newValue = 1
    newValue += listOf<Int>().asSequence().fold(0) { total, next ->
        call(<!ARGUMENT_TYPE_MISMATCH!>11<!>)
        total + next
    }
    newValue += listOf<Int>().asSequence().fold(0, fun(total, next): Int { return total + next })
    newValue += listOf<Int>().asSequence().fold(0, fun(total, next) = total + next)
    newValue += listOf<Int>().asSequence().fold(0, ::foo)

    emit(materialize<Bar>())

    newValue += listOf<Int>().asSequence().fold(0) { total, next -> total + next }
}
