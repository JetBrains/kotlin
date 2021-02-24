open class F
class B<out T>
class K<T>

private fun check(): () -> B<K<out F>> = {
    class Local : F()
    B<K<Local>>()
}