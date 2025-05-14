// FIR_IDENTICAL
// ISSUE: KT-68632

// MODULE: ZSocket
interface ZSocket {
    suspend fun funWithArg(arg: String)
    @Suppress("JS_NAME_CLASH")
    suspend fun funWithoutArgs()
}
// MODULE: ZAsyncSocket(ZSocket)
@OptIn(ExperimentalJsExport::class)
@JsExport
@JsName("ZSocket")
interface ZAsyncSocket {
    @JsName("funWithArg")
    fun funWithArgAsync(arg: String)

    @JsName("funWithoutArgs")
    fun funWithoutArgsAsync()
}

internal class ZAsyncSocketImpl(private val delegate: ZSocket) : ZSocket by delegate, ZAsyncSocket {
    override fun funWithArgAsync(arg: String) {}
    @Suppress("JS_NAME_CLASH")
    override fun funWithoutArgsAsync() {}
}
