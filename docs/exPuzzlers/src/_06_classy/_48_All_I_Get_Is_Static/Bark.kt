namespace all.i.get.`is`.static

import std.io.*
import std.*

open class Dog() {
    class object {
        fun bark() {
            print("woof")
        }
    }
}

class Basenji() : Dog() {
    class object {
        fun bark() { }
    }
}

fun main(args : Array<String>) {
    val woofer : Dog = Dog()
    val nipper : Dog = Basenji()
// Problematic code does not compile:
//    woofer.bark()
//    nipper.bark()
    Dog.bark()
    Basenji.bark()
}
