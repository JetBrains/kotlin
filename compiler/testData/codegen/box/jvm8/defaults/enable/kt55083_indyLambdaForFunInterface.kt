// JVM_DEFAULT_MODE: enable

interface Base {
    fun f(): String
}

sealed interface A : Base {
    override fun f(): String = "OK"

    fun interface B : A {
        operator fun invoke()
    }
}

val Impl = A.B { }

fun box(): String = Impl.f()
