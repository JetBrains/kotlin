package whats.the.difference

import java.util.HashSet

fun iarray(vararg a : Int) = a // BUG

fun IntArray.lastIndex() = size - 1

fun box() : String {
    // Problematic code does not compile
//    val vals = iarray(789, 678, 567, 456, 345, 234, 123, 012)

    val vals = iarray(789, 678, 567, 456, 345, 234, 123, 12)
    val diffs = HashSet<Int>()
    for (i in vals.indices)
        for (j in i..vals.lastIndex())
             diffs.add(vals[i] - vals[j])
    System.out?.println(diffs.size())

    return "OK"
}
