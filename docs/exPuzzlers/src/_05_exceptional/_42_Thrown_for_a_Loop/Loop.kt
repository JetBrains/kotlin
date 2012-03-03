namespace thrown.`for`.a.looop // BUG

import kotlin.io.*
import kotlin.*

fun iarr(vararg a : Int) = a // due to a BUG

fun main(args : Array<String>) {
    val tests = array(
        iarr(6, 5, 4, 3, 2, 1), iarr(1, 2),
        iarr(1, 2, 3), iarr(1, 2, 3, 4), iarr(1)
    )

    var n = 0

    try {
        var i = 0
        while (true) {
            if (thirdElementIsThree(tests[i++]))
              n++
        }
    }
    catch (e : ArrayIndexOutOfBoundsException) {
        // No more tests to process
    }
    println(n)
}

fun thirdElementIsThree(a : IntArray) =
// Problematic code does not compile
//    a.size >= 3 & a[2] == 3
    a.size >= 3 && a[2] == 3