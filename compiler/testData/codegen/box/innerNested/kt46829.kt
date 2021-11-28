class Outer<T> {
    var <V> Inner<T, V>.prop: V
        get() = this.value
        set(value) {
            this.value = value
        }
}

class Inner<T, V>(
    val key: T,
    var value: V
)

fun box(): String {
    Outer<Boolean>().run {
        val i = Inner(true, false)
        i.prop = true
    }
    return "OK"
}