open class Base

interface Trait : Base {
    private val value : String
        get() = "OK"
        
    override fun toString() = object {
        fun foo() = value
    }.foo()
}

class Derived : Trait, Base()

fun box() = "${Derived()}"
