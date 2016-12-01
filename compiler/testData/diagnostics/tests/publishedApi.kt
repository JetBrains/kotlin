// !DIAGNOSTICS: -UNUSED_PARAMETER
<!NON_INTERNAL_PUBLISHED_API!>@kotlin.PublishedApi<!>
class A

@kotlin.PublishedApi
internal class B

<!NON_INTERNAL_PUBLISHED_API!>@kotlin.PublishedApi<!>
private class C


<!NON_INTERNAL_PUBLISHED_API!>@kotlin.PublishedApi<!>
fun a() {}

@kotlin.PublishedApi
internal fun b() {}

@kotlin.PublishedApi
internal fun c() {}


<!NON_INTERNAL_PUBLISHED_API!>@kotlin.PublishedApi<!>
var ap = 1

@kotlin.PublishedApi
internal var bp = 1

@kotlin.PublishedApi
internal var c = 1



class E {
    <!NON_INTERNAL_PUBLISHED_API!>@kotlin.PublishedApi<!>
    fun a() {}

    @kotlin.PublishedApi
    internal fun b() {}

    <!NON_INTERNAL_PUBLISHED_API!>@kotlin.PublishedApi<!>
    private fun c() {}

    <!NON_INTERNAL_PUBLISHED_API!>@kotlin.PublishedApi<!>
    protected fun d() {}


    <!NON_INTERNAL_PUBLISHED_API!>@kotlin.PublishedApi<!>
    val ap = 1

    @kotlin.PublishedApi
    internal val bp = 1

    <!NON_INTERNAL_PUBLISHED_API!>@kotlin.PublishedApi<!>
    protected val c = 1

    <!NON_INTERNAL_PUBLISHED_API!>@kotlin.PublishedApi<!>
    private val d = 1
}


class D <!NON_INTERNAL_PUBLISHED_API!>@kotlin.PublishedApi<!> constructor() {

    <!NON_INTERNAL_PUBLISHED_API!>@kotlin.PublishedApi<!>
    constructor(a: String) : this()

    <!NON_INTERNAL_PUBLISHED_API!>@kotlin.PublishedApi<!>
    private constructor(a: String, b: String): this()

    @kotlin.PublishedApi
    internal constructor(a: String, b: String, c: String): this()
}
