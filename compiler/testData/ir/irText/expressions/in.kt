// FIR_IDENTICAL
// SKIP_KLIB_TEST
// REASON: `@[UnsafeVariance]` is added extra to `element` parameter of `public abstract fun contains (element: E of kotlin.collections.Collection): kotlin.Boolean`
fun test1(a: Any, x: Collection<Any>) = a in x
fun test2(a: Any, x: Collection<Any>) = a !in x
fun <T> test3(a: T, x: Collection<T>) = a in x
fun <T> test4(a: T, x: Collection<T>) = a !in x