// IGNORE_BACKEND: JS_IR, WASM_JS

// KT-61141: getTag() throws kotlin.Exception instead of java.lang.Exception
// IGNORE_BACKEND: NATIVE

typealias Action<RenderingT> = (@UnsafeVariance RenderingT) -> Unit

data class Tag<out RenderingT>(val action: Action<RenderingT>)

fun getTag(): Tag<*> = throw Exception()
fun doAction() {
    getTag().action
}
