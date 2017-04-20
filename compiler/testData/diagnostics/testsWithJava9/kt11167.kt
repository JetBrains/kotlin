import java.util.stream.IntStream

fun foo(s: IntStream) {
    val n = 1000000000
    val delta = 1.0 / n
    val startTimeNanos = System.nanoTime()
    val pi = 4.0 * delta * s.mapToDouble { i ->
        val x = (i - 0.5) * delta
        1.0 / (1.0 + x * x)
    }.sum()
    val elapseTime = (System.nanoTime() - startTimeNanos) / 1e9
    println("Parallel Streams $pi $n $elapseTime")
}
