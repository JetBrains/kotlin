package source

interface MyIntf {
    val foo: Int
}

open class MyClass: MyIntf {
    val foo: Int = 1
}

object MyObj: MyIntf by MyClass()

