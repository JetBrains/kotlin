fun <K> extract(x: Out<K>) = x.get()

class Out<out T>(val x: T) {
    fun get() = x
}
