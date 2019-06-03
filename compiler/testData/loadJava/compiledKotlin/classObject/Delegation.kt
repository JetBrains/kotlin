package test

interface T {
    fun foo(): Int
}

class A : T {
    override fun foo(): Int = 42

    companion object : T by A()
}
