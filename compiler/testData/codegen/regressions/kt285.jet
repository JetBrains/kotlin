trait Trait {
    fun foo() = "O"
    fun toString() = "K"
}

class SimpleClass : Trait

class ComplexClass : Trait by SimpleClass() {
    override fun toString() = foo() + super.toString()
}

fun box() = ComplexClass().toString()
