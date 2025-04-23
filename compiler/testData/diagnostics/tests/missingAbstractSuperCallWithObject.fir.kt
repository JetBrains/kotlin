// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-76365

interface Foo {
    fun check(): String = "OK"
}
abstract class Base {
    abstract fun check(): String
}
abstract class Derived : Base(), Foo

object Derived2 : Derived() {
    override fun check(): String {
        super<Derived>.check()
        return super.check()
    }
}
