class A

class B {
    val memberVal: A = A()
    fun memberFun(): A = A()
}

val B.extensionVal: A
    get() = A()

fun B.extensionFun(): A = A()

fun test_1() {
    val extensionValRef = B::extensionVal
    val extensionFunRef = B::extensionFun
}
