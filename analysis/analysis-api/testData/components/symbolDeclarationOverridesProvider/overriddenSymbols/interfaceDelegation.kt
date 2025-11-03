interface I {
    fun foo()
}

class C(private val p: I) : I by p

// callable: /C.foo