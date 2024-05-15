// ISSUE: KT-67933

interface Flow<out T> {
    suspend fun collect(c: FlowCollector<T>)
}

fun interface FlowCollector<in T> {
    suspend fun emit(x: T)
}

fun interface Fn<in T> : (T) -> Unit {
    override fun invoke(x: T)
}

suspend fun <T> Flow<T>.foo(
    function: (T) -> Unit,
    funInterface: Fn<T>,
) {
    collect(function)
    collect(funInterface)
}
