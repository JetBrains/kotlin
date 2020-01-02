public interface Collector<T, R>

class A<out T> {
    fun foo(): T = null!!
}

public fun <T> toList(): Collector<T, A<T>> = null!!

interface Stream<T> {
    public fun <R> collect(collector: Collector<in T, R>): R
}
fun stream(): Stream<String> = null!!

fun main() {
    val stream: Stream<String> = stream()
    val xs = stream.collect(toList())
    xs.foo()
}