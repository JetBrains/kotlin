// WITH_STDLIB
// WORKS_WHEN_VALUE_CLASS
// LANGUAGE: +ValueClasses, +GenericInlineClassParameter

OPTIONAL_JVM_INLINE_ANNOTATION
value class InlinedComparable<T: Int>(val x: T) : Comparable<InlinedComparable<T>> {
    override fun compareTo(other: InlinedComparable<T>): Int {
        return x.compareTo(other.x)
    }
}

fun <T> generic(c: Comparable<T>, element: T) = c.compareTo(element)

interface Base<T> {
    fun Base<T>.foo(a: Base<T>, b: T): Base<T>
}

OPTIONAL_JVM_INLINE_ANNOTATION
value class InlinedBase<T: Int>(val x: T) : Base<InlinedBase<T>> {
        override fun Base<InlinedBase<T>>.foo(a: Base<InlinedBase<T>>, b: InlinedBase<T>): Base<InlinedBase<T>> {
            return if (a is InlinedBase<*>) InlinedBase((a.x + b.x) as T) else this
        }

        fun double(): InlinedBase<T> {
            return this.foo(this, this) as InlinedBase<T>
        }
    }

fun box(): String {
    val a = InlinedComparable(42)
    if (generic(a, a) != 0) return "Fail 1"

    val b = InlinedBase(3)
    if (b.double().x != 6) return "Fail 2"

    return "OK"
}