// WITH_SIGNATURES

class B<T>(val a: T)

interface IColl : Collection<B<Int>> {
    override fun contains(element: B<Int>): kotlin.Boolean
}
