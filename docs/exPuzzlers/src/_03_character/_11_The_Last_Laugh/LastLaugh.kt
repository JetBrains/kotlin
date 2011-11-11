namespace last.laugh

import std.io.*

fun main(args : Array<String>) {
    print("H" + "a")
    // Problematic case does not compile
//    System.out?.print('H' + 'a')

    // Solution
    print('H'.toString() + 'a')
}