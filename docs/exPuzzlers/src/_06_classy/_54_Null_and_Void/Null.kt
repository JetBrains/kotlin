namespace `null`.and.void

import kotlin.io.*
import kotlin.*

class Null() {
    class object {
        fun greet() {
            println("Hello world!")
        }
    }
}


fun main(args : Array<String>) {
// The problemati code does not compile:
//    (null as Null).greet()
    Null.greet()
}