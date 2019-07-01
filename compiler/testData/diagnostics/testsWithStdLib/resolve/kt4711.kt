// !WITH_NEW_INFERENCE
//KT-4711 Error type with no error reported from type inference

fun main() {
    val n = 100
    val delta = 1.0 / n
    val startTimeNanos = System.nanoTime()

    // the problem sits on the next line:
    val pi = 4.0.toDouble() * delta <!OI;OVERLOAD_RESOLUTION_AMBIGUITY!>*<!> (1..n).<!OI;TYPE_INFERENCE_CONFLICTING_SUBSTITUTIONS!>reduce<!>(
            {t, i ->
                val x = (i - 0.5) * delta
                <!NI;TYPE_MISMATCH, NI;TYPE_MISMATCH!>t + 1.0 / (1.0 + x * x)<!>

            })
    // !!! pi has error type here

    val elapseTime = (System.nanoTime() - startTimeNanos) / 1e9

    println("pi_sequential_reduce $<!OI;DEBUG_INFO_ELEMENT_WITH_ERROR_TYPE!>pi<!> $n $elapseTime")
}
