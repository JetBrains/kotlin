// DUMP_IR_DIFFERENCE: JVM
//  K/JVM throws actualized java.lang.Exception instead of kotlin.Exception

typealias Action<RenderingT> = (@UnsafeVariance RenderingT) -> Unit

data class Tag<out RenderingT>(val action: Action<RenderingT>)

fun getTag(): Tag<*> = throw Exception()
fun doAction() {
    getTag().action
}
