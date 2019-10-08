// IGNORE_BACKEND: JVM_IR
// Missing IMPLICIT_NOTNULL casts

class A<T> {
    fun add(element: T) {}
}

public fun <R : Any> foo(x: MutableCollection<in R>, block: java.util.AbstractList<R>) {
    x.add(block.get(0))
}

// 1 checkExpressionValueIsNotNull
// 0 checkNotNullExpressionValue
