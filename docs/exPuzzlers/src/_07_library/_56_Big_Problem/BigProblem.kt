namespace big.problem

import std.io.*
import std.*
import java.math.BigInteger

fun main(args : Array<String>) {
    val fiveThousand = "5000".bi()
    val fiftyThousand = "50000".bi()
    val fiveHundredThousand = "500000".bi()

    val total : BigInteger = "0".bi()//BigInteger.ZERO
    total + fiveThousand
    total + fiftyThousand
    total + fiveHundredThousand
    println(total) // No surprise

    var total1 : BigInteger = "0".bi()//BigInteger.ZERO
    total1 += fiveThousand
    total1 += fiftyThousand
    total1 += fiveHundredThousand
    println(total1) // Works
}

inline fun String.bi() : BigInteger = BigInteger(this)
inline fun BigInteger.plus(other : BigInteger) = this add other