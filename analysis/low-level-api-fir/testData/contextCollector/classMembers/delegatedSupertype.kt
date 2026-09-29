package test

interface MyInterface {
    fun foo(): Int
}

class Implementation : MyInterface {
    override fun foo(): Int = 1
}

fun createImplementation(): MyInterface = Implementation()

class Delegating : MyInterface by <expr>createImplementation()</expr> {
    val property: Int = 1

    fun member(): Int = property
}
