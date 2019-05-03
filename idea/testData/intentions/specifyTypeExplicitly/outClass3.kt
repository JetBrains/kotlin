open class F
class B<T>
class K<out T>

private fun check()<caret> = {
    class Local : F()
    B<K<Local>>()
}