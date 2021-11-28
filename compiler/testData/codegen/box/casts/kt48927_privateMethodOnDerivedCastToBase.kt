abstract class Base {
    private fun test(): String = "OK"

    fun test(d: Derived): String = (d as Base).test()
}

class Derived : Base()

fun box(): String = Derived().test(Derived())
