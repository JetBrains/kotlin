
import org.jetbrains.kotlin.scripts.*

// this script expected parameter num : Int

fun fib(n: Int): Int {
    val v = fibCombine( { fib(it) }, n)
    System.out.println("fib($n)=$v")
    return v
}

System.out.println("num: $num")
val result = fib(num)

