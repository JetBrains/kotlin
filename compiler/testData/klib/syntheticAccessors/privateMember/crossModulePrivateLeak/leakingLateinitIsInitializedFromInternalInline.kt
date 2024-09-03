// MODULE: lib
// FILE: OK.kt
class OK {
    private lateinit var x: String // never leaked
    lateinit var y: String // never leaked

    private lateinit var o: String
    lateinit var k: String

    private inline fun doInitializeAndReadXY(): String {
        if (::x.isInitialized) throw Error("Property 'x' already initialized")
        x = "X"
        if (!::x.isInitialized) throw Error("Property 'x' is not initialized")

        if (::y.isInitialized) throw Error("Property 'y' already initialized")
        y = "Y"
        if (!::y.isInitialized) throw Error("Property 'y' is not initialized")

        return x + y
    }

    internal fun initializeAndReadXY(): String = doInitializeAndReadXY()

    private inline fun doInitializeAndReadOK(): String {
        if (::o.isInitialized) throw Error("Property 'o' already initialized")
        o = "O"
        if (!::o.isInitialized) throw Error("Property 'o' is not initialized")

        if (::k.isInitialized) throw Error("Property 'k' already initialized")
        k = "K"
        if (!::k.isInitialized) throw Error("Property 'k' is not initialized")

        return o + k
    }

    internal inline fun initializeAndReadOK(): String = doInitializeAndReadOK()
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String = OK().initializeAndReadOK()
