namespace `do`.your.thing

import std.io.*
import std.*
import _06_classy._53_Do_Your_Thing.Thing

class MyThing(val arg : Int) : Thing(arg) {

}


fun main(args : Array<String>) {
    println(MyThing(10).arg)
}