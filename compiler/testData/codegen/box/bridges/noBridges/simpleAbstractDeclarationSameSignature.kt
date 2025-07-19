abstract class A {
    abstract fun foo(): String
}

abstract class B : A() {
}

class C : B() {
    override fun foo(): String = "OK"
}

fun box(): String {
    val c: A = C()
    return c.foo()
}

//
//abstract class A {
//    abstract fun foo(): String
//}
//
//abstract class B : A() {
//    override abstract fun foo(): String
//}
//
//fun box(): String {
//    return "OK"
//}