open class Base

trait Trait : Base {
    private val value : String
        get() = "OK"
        
    fun toString() = object {
        fun foo() = value
    }.foo()
}

class Derived : Trait, Base()

fun box() = "${Derived()}"
