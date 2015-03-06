package test

trait T {
    fun foo(): Int
}

class A : T {
    override fun foo(): Int = 42

    default object : T by A()
}
