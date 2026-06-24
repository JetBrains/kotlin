// ISSUE: KT-69305

interface Mapping<T> {
    fun map(x: String): T
}

class A<T> {
    inner class Impl : Mapping<T> {
        @Suppress("UNCHECKED_CAST")
        override fun map(x: String): T = x as T
    }
}

class B<T> : Mapping<T> by A<T>().Impl()

fun box(): String = B<String>().map("OK")
