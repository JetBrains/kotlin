// !DIAGNOSTICS: -UNUSED_PARAMETER
@kotlin.PublishedApi
class A

@kotlin.PublishedApi
internal class B

@kotlin.PublishedApi
private class C


@kotlin.PublishedApi
fun a() {}

@kotlin.PublishedApi
internal fun b() {}

@kotlin.PublishedApi
internal fun c() {}


@kotlin.PublishedApi
var ap = 1

@kotlin.PublishedApi
internal var bp = 1

@kotlin.PublishedApi
internal var c = 1



class E {
    @kotlin.PublishedApi
    fun a() {}

    @kotlin.PublishedApi
    internal fun b() {}

    @kotlin.PublishedApi
    private fun c() {}

    @kotlin.PublishedApi
    protected fun d() {}


    @kotlin.PublishedApi
    val ap = 1

    @kotlin.PublishedApi
    internal val bp = 1

    @kotlin.PublishedApi
    protected val c = 1

    @kotlin.PublishedApi
    private val d = 1
}


class D @kotlin.PublishedApi constructor() {

    @kotlin.PublishedApi
    constructor(a: String) : this()

    @kotlin.PublishedApi
    private constructor(a: String, b: String): this()

    @kotlin.PublishedApi
    internal constructor(a: String, b: String, c: String): this()
}
