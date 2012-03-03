namespace change

import java.math.BigDecimal
import kotlin.io.*

fun main(args : Array<String>) {
  // Easy to make BigDecimals user-friendly
  println(
    "2.00".bd - "1.00"
  )
}

val String.bd : BigDecimal get() = BigDecimal(this)

fun BigDecimal.minus(other : BigDecimal) = this.subtract(other)
fun BigDecimal.minus(other : String) = subtract(other.bd) // this can be omitted



