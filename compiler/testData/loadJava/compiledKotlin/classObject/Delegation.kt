package test

trait T {
    fun foo(): Int
}

class A : T {
    override fun foo(): Int = 42

    class object : T by A()
}
