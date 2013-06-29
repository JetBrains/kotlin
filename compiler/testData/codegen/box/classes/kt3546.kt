trait A {
    fun test(): String
}

trait B {
    fun test(): String
}

trait C: A, B

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