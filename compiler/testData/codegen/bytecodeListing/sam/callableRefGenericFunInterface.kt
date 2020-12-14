// WITH_SIGNATURES
// FILE: t.kt

fun interface Sam<T> {
    fun get(): T
}

fun <T> expectsSam(sam: Sam<T>) = sam.get()

fun <T> foo(): T = null!!

fun <T> genericSam(): T = expectsSam(::foo)
