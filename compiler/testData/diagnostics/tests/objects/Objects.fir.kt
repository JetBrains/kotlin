package toplevelObjectDeclarations

open class Foo(y: Int) {
    open fun foo(): Int = 1
}

class T : <!NO_VALUE_FOR_PARAMETER, SUPERTYPE_NOT_INITIALIZED!>Foo<!> {}

object A : <!NO_VALUE_FOR_PARAMETER, SUPERTYPE_NOT_INITIALIZED!>Foo<!> {
    val x: Int = 2

    fun test(): Int {
        return x + foo()
    }
}

object B : <!SINGLETON_IN_SUPERTYPE!>A<!> {}

val c = object : <!NO_VALUE_FOR_PARAMETER, SUPERTYPE_NOT_INITIALIZED!>Foo<!> {}

val x = A.foo()

val y = object : Foo(x) {
    init {
        x + 12
    }

    override fun foo(): Int = 1
}

val z = y.foo()
