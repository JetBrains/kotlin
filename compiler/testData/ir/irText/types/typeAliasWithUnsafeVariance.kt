// KT-42036
// IGNORE_BACKEND: JS_IR

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57429

typealias Action<RenderingT> = (@UnsafeVariance RenderingT) -> Unit

data class Tag<out RenderingT>(val action: Action<RenderingT>)

fun getTag(): Tag<*> = throw Exception()
fun doAction() {
    getTag().action
}
