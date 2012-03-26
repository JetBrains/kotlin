package userpackage

import testData.libraries.*

fun foo(a : ClassWithAbstractAndOpenMembers) {
    a.abstractVar = "v"
    println(a.abstractVar)
}

fun main(args : Array<String>) : Unit {
    val color: Color? = Color.RED
    color?.rgb

    println(testData.libraries.globalVal)
    println(testData.libraries.globalValWithGetter)
    println("".exProp)
    println(#(1, 2).exProp)
    func(5)
    func(5, "5")
    func(5, 5)
    func()
}