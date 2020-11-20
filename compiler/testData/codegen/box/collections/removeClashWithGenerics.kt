// TARGET_BACKEND: JVM
// WITH_RUNTUME

// See also: KT-42083

var removed = ""

class MyCharSequenceSet1 : Set<CharSequence> {
    override val size: Int get() = TODO()
    override fun contains(element: CharSequence): Boolean = TODO()
    override fun containsAll(elements: Collection<CharSequence>): Boolean = TODO()
    override fun isEmpty(): Boolean = TODO()
    override fun iterator(): Iterator<CharSequence> = TODO()

    fun <Q : CharSequence> remove(cs: Q): Boolean {
        removed = cs.toString()
        return false
    }
}

class MyCharSequenceSet2 : Set<CharSequence> {
    override val size: Int get() = TODO()
    override fun contains(element: CharSequence): Boolean = TODO()
    override fun containsAll(elements: Collection<CharSequence>): Boolean = TODO()
    override fun isEmpty(): Boolean = TODO()
    override fun iterator(): Iterator<CharSequence> = TODO()

    fun <Q : CharSequence> remove(cs: Q?): Boolean {
        removed = cs.toString()
        return false
    }
}

fun box(): String {
    val s1 = MyCharSequenceSet1()
    s1.remove("OK")
    if (removed != "OK") throw AssertionError()

    try {
        (s1 as java.util.Set<CharSequence>).remove("OK")
        throw AssertionError("(s1 as java.util.Set<CharSequence>) should throw UnsupportedOperationException")
    } catch (e: UnsupportedOperationException) {
    } catch (e: Throwable) {
        throw AssertionError("(s1 as java.util.Set<CharSequence>) should throw UnsupportedOperationException")
    }

    removed = ""

    val s2 = MyCharSequenceSet2()
    s2.remove("OK")
    if (removed != "OK") throw AssertionError()

    try {
        (s2 as java.util.Set<CharSequence>).remove("OK")
        throw AssertionError("(s2 as java.util.Set<CharSequence>) should throw UnsupportedOperationException")
    } catch (e: UnsupportedOperationException) {
    } catch (e: Throwable) {
        throw AssertionError("(s2 as java.util.Set<CharSequence>) should throw UnsupportedOperationException")
    }

    return "OK"
}