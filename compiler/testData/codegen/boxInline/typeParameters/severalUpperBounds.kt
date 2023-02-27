// FILE: 1.kt
interface I {
    fun f(): String
}

interface J {
    fun f2(): String
}

class X : I, J {
    override fun f(): String = "O"
    override fun f2(): String = "K"
}

class Box<T>(val x: T) where T: Any, T : I, T: J {
    inline fun f(): String {
        val a = x
        return a.f() + a.f2()
    }
}

// FILE: 2.kt
fun box(): String {
    return Box(X()).f()
}