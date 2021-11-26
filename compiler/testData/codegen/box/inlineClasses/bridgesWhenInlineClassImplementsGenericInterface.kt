// WITH_STDLIB

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class InlinedComparable(val x: Int) : Comparable<InlinedComparable> {
    override fun compareTo(other: InlinedComparable): Int {
        return x.compareTo(other.x)
    }
}

fun <T> generic(c: Comparable<T>, element: T) = c.compareTo(element)

interface Base<T> {
    fun Base<T>.foo(a: Base<T>, b: T): Base<T>
}

@Suppress("OPTIONAL_DECLARATION_USAGE_IN_NON_COMMON_SOURCE")
@kotlin.jvm.JvmInline
value class InlinedBase(val x: Int) : Base<InlinedBase> {
    override fun Base<InlinedBase>.foo(a: Base<InlinedBase>, b: InlinedBase): Base<InlinedBase> {
        return if (a is InlinedBase) InlinedBase(a.x + b.x) else this
    }

    fun double(): InlinedBase {
        return this.foo(this, this) as InlinedBase
    }
}

fun box(): String {
    val a = InlinedComparable(42)
    if (generic(a, a) != 0) return "Fail 1"

    val b = InlinedBase(3)
    if (b.double().x != 6) return "Fail 2"

    return "OK"
}