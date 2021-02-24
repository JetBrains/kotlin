// !WITH_NEW_INFERENCE
//KT-4711 Error type with no error reported from type inference

fun main() {
    val n = 100
    val delta = 1.0 / n
    val startTimeNanos = System.nanoTime()

    // the problem sits on the next line:
    val pi = 4.0.toDouble() * delta <!OVERLOAD_RESOLUTION_AMBIGUITY{OI}!>*<!> (1..n).<!TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS{OI}!>reduce<!>(
            {t, i ->
                val x = (i - 0.5) * delta
                <!TYPE_MISMATCH{NI}, TYPE_MISMATCH{NI}, TYPE_MISMATCH{NI}!>t + 1.0 / (1.0 + x * x)<!>

            })
    // !!! pi has error type here

    val elapseTime = (System.nanoTime() - startTimeNanos) / 1e9

    println("pi_sequential_reduce $<!DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE{OI}!>pi<!> $n $elapseTime")
}
