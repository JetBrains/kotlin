// WITH_STDLIB
interface A<T> {
    suspend fun foo(): T
}

interface B<K> : A<K>

abstract class C<V> : B<V> {
    override suspend fun foo(): V = TODO()
}
