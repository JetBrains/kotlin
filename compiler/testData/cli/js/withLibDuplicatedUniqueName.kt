import kotlin.Pair
import library.sample.*

fun main() {
    val p = Pair(10, 20)
    val x = pairAdd(p)
    println("pairAdd(p)=$x")
    val y = pairSub(p)
    println("pairSub(y)=$y")
}