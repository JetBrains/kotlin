// FILE: OK.kt
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

// FILE: main.kt
fun box(): String = OK().initializeAndReadLateinitProperties()
