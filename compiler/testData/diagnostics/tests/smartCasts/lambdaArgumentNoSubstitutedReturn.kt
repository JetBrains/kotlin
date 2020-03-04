// FIR_IDENTICAL
class Indexed<T>(val x: T, val y: Int)

class Value<out T>(val x: T)

interface WithValue<out T> {
    fun value(): Value<T>
}

class Singleton<T>(val x: T) : WithValue<T> {
    override fun value() = Value(x)
}

class WithValueIndexed<T>(val f: () -> Value<T>) : WithValue<Indexed<T>> {
    override fun value() = Value(Indexed(f().x, 0))
}

fun <T> Singleton<out T>.indexed(): WithValue<Indexed<T>> {
    return WithValueIndexed { value() }
}
