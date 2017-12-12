package javaSamFunction

import forTests.MyJavaClass

fun main(args: Array<String>) {
    val klass = MyJavaClass()
    //Breakpoint!
    klass.other { /* do nothing*/ }
}