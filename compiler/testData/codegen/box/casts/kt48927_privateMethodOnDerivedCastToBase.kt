abstract class Base {
    private fun test(): String = "OK"

    fun test(d: Derived): String = (d as Base).test()

    fun test(d: Array<out Derived>) = (d as Array<out Base>)[0].test()
}

class Derived : Base()

fun box(): String {
    Derived().test(arrayOf(Derived()))
    return Derived().test(Derived())
}
