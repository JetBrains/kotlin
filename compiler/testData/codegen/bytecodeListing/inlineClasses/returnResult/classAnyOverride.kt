// WITH_RUNTIME

interface I {
    fun foo(): Any
}

class C : I {
    override fun foo(): Result<Boolean> = TODO()
}

