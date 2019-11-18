// IGNORE_BACKEND_FIR: JVM_IR
interface A {
    fun test(): String
}

interface B {
    fun test(): String
}

interface C: A, B

class Z(val param: String): C {

    override fun test(): String {
        return param
    }
}

public class MyClass(val value : C) : C by value {

}

fun box(): String {
    val s = MyClass(Z("OK"))
    return s.test()
}