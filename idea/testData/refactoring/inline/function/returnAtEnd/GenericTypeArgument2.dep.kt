package test.pack.one

interface SomeFace
interface GeneOut<out T> {}
object Empty : GeneOut<Nothing>
fun <T> downUnder(): GeneOut<T> = Empty
fun downParameter(p: GeneOut<SomeFace>): GeneOut<SomeFace> = p