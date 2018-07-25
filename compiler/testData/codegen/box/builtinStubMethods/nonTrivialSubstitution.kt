// IGNORE_BACKEND: JVM_IR
// IGNORE_BACKEND: JS_IR
// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

class MyCollection<T> : Collection<List<Iterator<T>>> {
    override fun iterator() = null!!
    override val size: Int get() = null!!
    override fun isEmpty(): Boolean = null!!
    override fun contains(o: List<Iterator<T>>): Boolean = null!!
    override fun containsAll(c: Collection<List<Iterator<T>>>): Boolean = null!!
}

fun box(): String {
    val c = MyCollection<String>() as java.util.Collection<List<Iterator<String>>>
    try {
        c.add(ArrayList())
        return "Fail"
    } catch (e: UnsupportedOperationException) {
        return "OK"
    }
}
