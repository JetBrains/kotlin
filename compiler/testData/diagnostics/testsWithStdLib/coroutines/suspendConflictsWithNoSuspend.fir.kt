// FILE: main.kt
interface A {
    suspend fun foo()
    fun foo()
}

interface B : A {
    suspend override fun foo() {

    }

    override fun foo() {

    }
}
