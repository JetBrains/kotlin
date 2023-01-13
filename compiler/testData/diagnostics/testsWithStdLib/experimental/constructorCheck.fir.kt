@RequiresOptIn
annotation class Marker

@Marker
class Some(val x: Int)

class Other(val x: Int) {
    @OptIn(Marker::class)
    constructor(some: Some): this(some.x)

    @Marker
    constructor(): this(42)

    @OptIn(Marker::class)
    constructor(y: Long, some: Some? = null): this(some?.x ?: y.toInt())
}

fun foo(some: <!OPT_IN_USAGE_ERROR!>Some<!>? = null) {}

fun test() {
    val o1 = <!OPT_IN_USAGE_ERROR!>Other<!>()
    val o2 = <!OPT_IN_USAGE_ERROR!>Other<!>(<!OPT_IN_USAGE_ERROR!>Some<!>(0))
    val o3 = <!OPT_IN_USAGE_ERROR!>Other<!>(444L)
    <!OPT_IN_USAGE_ERROR!>foo<!>()
    <!OPT_IN_USAGE_ERROR!>foo<!>(null)
}
