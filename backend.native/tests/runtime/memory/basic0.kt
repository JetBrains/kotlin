class A {
    var field: B? = null
}

class B(var field: Int)

fun main(args : Array<String>) {
    val a = A()
    a.field = B(2)
}
