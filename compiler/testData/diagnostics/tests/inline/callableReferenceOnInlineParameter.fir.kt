// RUN_PIPELINE_TILL: BACKEND
fun interface Warp<A, B> {
    fun apply(input: A): B

    companion object {
        inline fun <A, B> create(f: (A) -> B): Warp<A, B> = Warp(f::invoke)
    }
}
