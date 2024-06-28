// ISSUE: KT-59541
interface WriteContext {
    val x: String
}

interface ReadContext {
    val y: String
}

interface Codec<T> {
    fun WriteContext.encode(value: T)
    fun ReadContext.decode(): T?
}

fun <T> codec(
    encode: WriteContext.(T) -> Unit,
    decode: ReadContext.() -> T?
): Codec<T> = object : Codec<T> {
    // Mostly, we check in this test that both `encode(value)` and `decode()` are resolved
    // to the corresponding parameters of `codec` function (see KT-59541)
    // Because before the fix for KT-37375, they both were resolved to the members of the anonymous objects
    // leading to recursion and stack overflow.
    override fun WriteContext.encode(value: T) = encode(value)
    override fun ReadContext.decode(): T? = decode()
}

fun box(): String {
    var result = ""

    val t = codec(
        {
            result += x + it
        },
        {
            y
        }
    )



    with(t) {
        object : WriteContext {
            override val x: String
                get() = "O"
        }.encode("K")

        result += object : ReadContext {
            override val y: String
                get() = "123"
        }.decode()
    }

    if (result != "OK123") return "fail: $result"
    return "OK"
}
