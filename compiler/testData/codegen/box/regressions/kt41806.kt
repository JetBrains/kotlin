// TARGET_BACKEND: JVM
// WITH_RUNTIME
// IGNORE_BACKEND_FIR: JVM_IR

open class A {
    fun Foo() {
        print("Fop")
    }
}
class B() : A()

class C() : A()

fun box(): String {
    var test : A = B()

    println((test as B).toString())

    listOf(1,2 ,3).forEach { it ->
        test = C()
    }

    test.Foo()

    return "OK"
}