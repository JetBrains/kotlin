// TARGET_BACKEND: JVM
// WITH_STDLIB

var removed: String? = ""

open class RemoveStringNImpl {
    fun remove(s: String?): Boolean {
        removed = s
        return false
    }
}

class S1 : Set<String>, RemoveStringNImpl() {
    override val size: Int get() = TODO()
    override fun contains(element: String): Boolean = TODO()
    override fun containsAll(elements: Collection<String>): Boolean = TODO()
    override fun isEmpty(): Boolean = TODO()
    override fun iterator(): Iterator<String> = TODO()
}

class S2 : Set<String> {
    override val size: Int get() = TODO()
    override fun contains(element: String): Boolean = TODO()
    override fun containsAll(elements: Collection<String>): Boolean = TODO()
    override fun isEmpty(): Boolean = TODO()
    override fun iterator(): Iterator<String> = TODO()

    fun remove(s: String?): Boolean {
        removed = s
        return false
    }
}

class S3 : Set<String> {
    override val size: Int get() = 0
    override fun contains(element: String): Boolean = false
    override fun containsAll(elements: Collection<String>): Boolean = false
    override fun isEmpty(): Boolean = true
    override fun iterator(): Iterator<String> = emptyList<String>().iterator()

    fun remove(s: String) = s
}

fun javaSetRemoveShouldThrowUOE(message: String, s: Set<String>) {
    try {
        (s as java.util.Set<String>).remove("")
        throw AssertionError(message)
    } catch (e: UnsupportedOperationException) {
    }
}

fun box(): String {
    javaSetRemoveShouldThrowUOE("(S1 as java.util.Set).remove", S1())

    javaSetRemoveShouldThrowUOE("(S2 as java.util.Set).remove", S2())

    removed = ""
    S1().remove("OK")
    if (removed != "OK") throw AssertionError("S1.remove")

    removed = ""
    S2().remove("OK")
    if (removed != "OK") throw AssertionError("S2.remove")

    if (S3().remove("OK") != "OK") throw AssertionError("S3.remove")

    return "OK"
}
