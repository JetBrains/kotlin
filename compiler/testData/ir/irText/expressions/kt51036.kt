// WITH_STDLIB
// SKIP_KT_DUMP

class A {
    companion object;
    operator fun String.invoke() = Unit
    fun close() = synchronized(this) { "Abc" }()
}
