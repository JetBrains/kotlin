val String.test1: Int
var String.test2: Int

var String.test3: Int; public set

class C {
    val String.test1: Int
    var String.test2: Int
    var String.test3: Int; public set
}

interface I {
    val String.test1: Int
    var String.test2: Int
    var String.test3: Int; public set
}

abstract class A {
    val String.test1: Int
    var String.test2: Int
    var String.test3: Int; public set

    abstract val String.testA1: Int
    abstract var String.testA2: Int
}