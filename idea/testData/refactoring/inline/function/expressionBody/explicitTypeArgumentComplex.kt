interface SomeFace
interface GeneOut<out T> {}
object Empty : GeneOut<Nothing>
fun <T> downUnder(): GeneOut<T> = Empty
fun downParameter(p: GeneOut<SomeFace>) = p

fun callDown() {
    // BUG: KT-17402
    val v2 = <caret>downParameter(downUnder())
}