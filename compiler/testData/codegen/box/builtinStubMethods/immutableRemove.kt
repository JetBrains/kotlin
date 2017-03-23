// TODO: muted automatically, investigate should it be ran for JS or not
// IGNORE_BACKEND: JS, NATIVE

// FULL_JDK
// WITH_RUNTIME
interface ImmutableCollection<out E> : Collection<E> {
    fun add(element: @UnsafeVariance E): ImmutableCollection<E>
    fun addAll(elements: Collection<@UnsafeVariance E>): ImmutableCollection<E>
    fun remove(element: @UnsafeVariance E): ImmutableCollection<E>
}

class ImmutableCollectionmpl<E> : ImmutableCollection<E> {
    override val size: Int
        get() = throw UnsupportedOperationException()

    override fun contains(element: E): Boolean {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun containsAll(elements: Collection<E>): Boolean {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun isEmpty(): Boolean {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun iterator(): Iterator<E> {
        throw UnsupportedOperationException("not implemented") //To change body of created functions use File | Settings | File Templates.
    }

    override fun add(element: E): ImmutableCollection<E> = this
    override fun addAll(elements: Collection<E>): ImmutableCollection<E> = this
    override fun remove(element: E): ImmutableCollection<E> = this
}

fun box(): String {
    val c = ImmutableCollectionmpl<String>()
    if (c.remove("") !== c) return "fail 1"
    if (c.add("") !== c) return "fail 2"
    if (c.addAll(java.util.ArrayList()) !== c) return "fail 3"

    val method = c.javaClass.methods.single { it.name == "remove" && it.returnType == Boolean::class.javaPrimitiveType }

    try {
        method.invoke(c, "")
        return "fail 4"
    } catch (e: java.lang.reflect.InvocationTargetException) {
        if (e.cause!!.message != "Operation is not supported for read-only collection") return "fail 5: ${e.cause!!.message}"
    }

    return "OK"
}
