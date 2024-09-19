// MODULE: lib
// FILE: OK.kt
@Suppress("LATEINIT_INTRINSIC_CALL_IN_INLINE_FUNCTION", "LATEINIT_INTRINSIC_CALL_ON_NON_ACCESSIBLE_PROPERTY")
class OK {
    private lateinit var o: String
    lateinit var k: String

    internal inline fun initializeAndReadLateinitProperties(): String {
        if (::o.isInitialized) throw Error("Property 'o' already initialized")
        o = "O"
        if (!::o.isInitialized) throw Error("Property 'o' is not initialized")

        if (::k.isInitialized) throw Error("Property 'k' already initialized")
        k = "K"
        if (!::k.isInitialized) throw Error("Property 'k' is not initialized")

        return o + k
    }
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String = OK().initializeAndReadLateinitProperties()
