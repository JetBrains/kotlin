// WITH_STDLIB
interface A {
    suspend fun foo(): Boolean
}

class B : A {
    suspend override fun foo(): Boolean = true
}

open class C {
    suspend fun foo(): Boolean = true
}

class D : C(), A
