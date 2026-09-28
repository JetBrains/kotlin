// I
// LANGUAGE: +CompanionBlocks
// JVM_DEFAULT_MODE: disable

interface I<T> {
    fun member(x: T): T = x
    fun abstractMember(): T

    companion {
        fun foo(x: String): String = x
        val value: String get() = "OK"
    }
}
