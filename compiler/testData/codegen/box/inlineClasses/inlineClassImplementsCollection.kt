// !LANGUAGE: +InlineClasses
// IGNORE_BACKEND_FIR: JVM_IR
// WITH_RUNTIME
// TARGET_BACKEND: JVM

inline class MyUInt(val x: Int)

inline class MyUIntArray(private val storage: IntArray) : Collection<MyUInt> {
    public override val size: Int get() = storage.size

    override operator fun iterator() = TODO()
    override fun contains(element: MyUInt): Boolean = storage.contains(element.x)
    override fun containsAll(elements: Collection<MyUInt>): Boolean = elements.all { storage.contains(it.x) }
    override fun isEmpty(): Boolean = TODO()
}

fun <T> checkBoxed(c: Collection<T>, element: T): Boolean {
    return c.contains(element) && c.containsAll(listOf(element))
}

fun box(): String {
    val uints = MyUIntArray(intArrayOf(0, 1, 42))

    if (MyUInt(42) !in uints) return "Fail 1"

    val ints = listOf(MyUInt(1), MyUInt(0))
    if (!uints.containsAll(ints)) return "Fail 2"

    if (!checkBoxed(uints, MyUInt(0))) return "Fail 3"

    return "OK"
}