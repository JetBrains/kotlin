// DIAGNOSTICS: -UNUSED_PARAMETER
// WITH_STDLIB
// IGNORE_KLIB_BACKEND_ERRORS_WITH_CUSTOM_FIRST_STAGE: Wasm-JS:2.0,2.1
// ^^^ KT-86180: CompileError: WebAssembly.Module(): Compiling function #3779:"CommonCase$<get-test1>.invoke" failed: call[0] expected type (ref null 997), found struct.new of type (ref 694) @+327498

object CommonCase {
    interface Fas<D, E, R>

    fun <D, E, R> delegate() : Fas<D, E, R> = object : Fas<D, E, R> {}

    operator fun <D, E, R> Fas<D, E, R>.provideDelegate(host: D, p: Any?): Fas<D, E, R> = this
    operator fun <D, E, R> Fas<D, E, R>.getValue(receiver: E, p: Any?): R = "OK" as R

    val Long.test1: String by delegate()
    val Long.test2: String by delegate<CommonCase, Long, String>()
}

fun box() = with(CommonCase) {
    require(3L.test1 == "OK" && 3L.test2 == "OK")
    "OK"
}
