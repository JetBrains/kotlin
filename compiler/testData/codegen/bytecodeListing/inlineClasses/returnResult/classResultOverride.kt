// WITH_STDLIB

interface I {
    fun foo(): Result<Boolean>
}

class C : I {
    override fun foo(): Result<Boolean> = TODO()
}

