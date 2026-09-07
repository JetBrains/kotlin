// I
// LANGUAGE: +CompanionBlocks
// LIBRARY_PLATFORMS: JVM
// JVM_DEFAULT_MODE: disable
// COMPILER_ARGUMENTS: -jvm-default=disable

interface I<T> {
    fun member(x: T): T = x
    fun abstractMember(): T

    companion {
        fun foo(x: String): String = x
        val value: String get() = "OK"
    }
}
