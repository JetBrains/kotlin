// !CHECK_TYPE


@kotlin.internal.<!INVISIBLE_REFERENCE, INVISIBLE_REFERENCE!>InlineOnly<!>
public inline fun <C, R> C.ifEmpty(f: () -> R): R where C : Collection<*>, C : R = if (isEmpty()) f() else this

public fun <T> listOf(t: T): List<T> = TODO()


fun usage(c: List<String>) {
    val cn = c.ifEmpty { null }
    cn checkType { _<List<String>?>() }

    val cs = c.ifEmpty { listOf("x") }
    cs checkType { _<List<String>>() }
}
