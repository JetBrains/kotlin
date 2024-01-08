public fun <R : Any> foo(x: MutableCollection<in R>, block: java.util.AbstractList<R>) {
    x.add(block.get(0))
}

// 0 checkExpressionValueIsNotNull
// 1 checkNotNullExpressionValue
