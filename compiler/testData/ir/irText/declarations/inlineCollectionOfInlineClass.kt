// FIR_IDENTICAL
// KT-64271
// IGNORE_BACKEND_K1: JS_IR, JS_IR_ES6
// IGNORE_BACKEND_K2: JVM_IR, JS_IR, JS_IR_ES6
// ^ Set has js specific methods

inline class IT(val x: Int)

inline class InlineMutableSet(private val ms: MutableSet<IT>) : MutableSet<IT> {
    override val size: Int get() = ms.size
    override fun contains(element: IT): Boolean = ms.contains(element)
    override fun containsAll(elements: Collection<IT>): Boolean = ms.containsAll(elements)
    override fun isEmpty(): Boolean = ms.isEmpty()
    override fun add(element: IT): Boolean = ms.add(element)
    override fun addAll(elements: Collection<IT>): Boolean = ms.addAll(elements)
    override fun clear() { ms.clear() }
    override fun iterator(): MutableIterator<IT> = ms.iterator()
    override fun remove(element: IT): Boolean = ms.remove(element)
    override fun removeAll(elements: Collection<IT>): Boolean = ms.removeAll(elements)
    override fun retainAll(elements: Collection<IT>): Boolean = ms.retainAll(elements)
}
