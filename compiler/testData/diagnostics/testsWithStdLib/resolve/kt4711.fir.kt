// !WITH_NEW_INFERENCE
//KT-4711 Error type with no error reported from type inference

fun main() {
    val n = 100
    val delta = 1.0 / n
    val startTimeNanos = System.nanoTime()

    // the problem sits on the next line:
    val pi = 4.0.toDouble() * delta * (1..n).reduce(
            {t, i ->
                val x = (i - 0.5) * delta
                <!ARGUMENT_TYPE_MISMATCH, TYPE_MISMATCH!>t + 1.0 / (1.0 + x * x)<!>

            })
    // !!! pi has error type here

    val elapseTime = (System.nanoTime() - startTimeNanos) / 1e9

    println("pi_sequential_reduce $pi $n $elapseTime")
}
