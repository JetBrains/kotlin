// KT-42036
// IGNORE_BACKEND: JS_IR

typealias Action<RenderingT> = (@UnsafeVariance RenderingT) -> Unit

data class Tag<out RenderingT>(val action: Action<RenderingT>)

fun getTag(): Tag<*> = throw Exception()
fun doAction() {
    getTag().action
}
