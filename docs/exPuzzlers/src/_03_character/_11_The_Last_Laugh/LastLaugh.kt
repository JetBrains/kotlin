namespace last.laugh

import kotlin.io.*

fun main(args : Array<String>) {
    print("H" + "a")
    // Problematic case does not compile
//    System.out?.print('H' + 'a')

    // Solution
    print('H'.toString() + 'a')
}