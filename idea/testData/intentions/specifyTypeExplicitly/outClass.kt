open class F
class B<out T>
class K<out T>

private fun check()<caret> = {
    class Local : F()
    B<K<Local>>()
}