
import org.jetbrains.kotlin.scripts.*

// this script expected parameter num : Int

fun fib(n: Int): Int {
    val v = fibCombine( { fib(it) }, n)
    println("fib($n)=$v")
    return v
}

println("num: $num")
val result = fib(num)

