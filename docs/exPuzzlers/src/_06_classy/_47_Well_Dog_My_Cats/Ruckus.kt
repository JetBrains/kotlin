namespace well.dog.my.cats

import std.io.*
import std.*

open class Counter {
    class object {
        private var count = 0
        fun increment() { count++ }
        fun getCount() = count
    }
}

class Dog() : Counter {
// Class objects are not inherited:
//    fun woof() { increment() }
    fun woof() { Counter.increment() }
}

class Cat() : Counter {
// Class objects are not inherited:
//    fun meow() { increment() }
    fun meow() { Counter.increment() }
}

fun main(args : Array<String>) {
    val dogs = array(Dog(), Dog())
    for (dog in dogs)
      dog.woof()
    val cats = array(Cat(), Cat())
    for (cat in cats)
      cat.meow()
// Problematic code does not compile:
//    println("${Dog.getCount()} woofs")
    println("${Counter.getCount()} woofs")
//    println("${Cat.getCount()} meows")
    println("${Counter.getCount()} meows")
}