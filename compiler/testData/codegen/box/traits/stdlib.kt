trait ISized {
    val size : Int
}

trait javaUtilIterator<T> : Iterator<T> {
    fun remove() : Unit {
        throw UnsupportedOperationException()
    }
}

class MyIterator<T>(val array : ReadOnlyArray<T>) : javaUtilIterator<T> {
    private var index = 0

    override fun hasNext() : Boolean = index < array.size

    override fun next() : T = array.get(index++)
}

trait ReadOnlyArray<out T> : ISized, Iterable<T> {
  fun get(index : Int) : T

  override fun iterator() : Iterator<T> = MyIterator<T>(this)
}

trait WriteOnlyArray<in T> : ISized {
  fun set(index : Int, value : T) : Unit

  fun set(from: Int, count: Int, value: T) {
    for(i in from..from+count-1) {
        set(i, value)
    }
  }
}

class MutableArray<T>(length: Int, init : (Int) -> T) : ReadOnlyArray<T>, WriteOnlyArray<T> {
    private val array = Array<Any?>(length, init)

    override fun get(index : Int) : T = array[index] as T
    override fun set(index : Int, value : T) : Unit { array[index] = value }

    override val size : Int
        get() = array.size()
}

fun box() : String {
    var a = MutableArray<Int> (4, {0})
    a [0] = 10
    a.set(1, 2, 13)
    a [3] = 40
    a.iterator()
    a.iterator().hasNext()
    for(el in a) {
        val fl = el
    }
    return "OK"
}
