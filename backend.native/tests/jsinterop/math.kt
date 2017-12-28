import kotlinx.interop.wasm.math.*
import kotlinx.wasm.jsinterop.*

fun main(args: Array<String>) {

    val e = Math.E
    val pi = Math.PI

    val sin_pi = Math.sin(pi)
    val sin_pi_2 = Math.sin(pi/2)
    val ln_1 = Math.log(1.0)
    val ln_e = Math.log(e)

    println("e = $e, pi = $pi, sin(pi) = $sin_pi, sin(pi/2) = $sin_pi_2, ln(1) = $ln_1, ln(e) = $ln_e")
}

