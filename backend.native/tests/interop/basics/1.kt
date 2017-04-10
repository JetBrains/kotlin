import cstdlib.*
import kotlinx.cinterop.*

fun main(args: Array<String>) {
    println(atoi("257"))

    val divResult = div(-5, 3)
    val (quot, rem) = divResult.useContents { Pair(quot, rem) }
    println(quot)
    println(rem)

}