// !LANGUAGE: +NewInference

// KT-42036

typealias Action<RenderingT> = (@UnsafeVariance RenderingT) -> Unit
// When a typealias is used, the compiler crashes.
data class Tag<out RenderingT>(val action: Action<RenderingT>)
// When no typealias is used, the compiler is fine.
//data class Tag<out RenderingT>(val action: (@UnsafeVariance RenderingT) -> Unit)
fun getTag(): Tag<*> = throw Exception()
fun doAction() {
    // This line crashes the compiler.
    getTag().action
}