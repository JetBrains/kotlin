// KT-72862: Undefined symbols for architecture arm64: "_kfun:#$OK.doInitializeAndReadOK.<no name provided>.ok_field(OK#doInitializeAndReadOK.<no name provided>){}kotlin.String?"
// IGNORE_NATIVE: cacheMode=STATIC_EVERYWHERE
// IGNORE_NATIVE: cacheMode=STATIC_PER_FILE_EVERYWHERE
// KT-72862: No function found for symbol
// IGNORE_NATIVE: cacheMode=STATIC_USE_HEADERS_EVERYWHERE

// IGNORE_BACKEND: JVM_IR
// WITH_STDLIB
// MODULE: lib
// FILE: OK.kt
@Suppress("LATEINIT_INTRINSIC_CALL_IN_INLINE_FUNCTION", "LATEINIT_INTRINSIC_CALL_ON_NON_ACCESSIBLE_PROPERTY")
class OK {
    private lateinit var x: String // never leaked
    lateinit var y: String // never leaked

    private lateinit var o: String
    lateinit var k: String

    private inline fun doInitializeAndReadXY(): String {
        if (::x.isInitialized) throw Error("Property 'x' already initialized")
        x = "X"
        if (!::x.isInitialized) throw Error("Property 'x' is not initialized")

        object {
            fun run() {
                if (::y.isInitialized) throw Error("Property 'y' already initialized")
                y = "Y"
                if (!::y.isInitialized) throw Error("Property 'y' is not initialized")
            }
        }
        val local = object {
            lateinit var xy: String
        }

        if (local::xy.isInitialized) throw Error("Property 'xy' already initialized")
        local.xy = x + y
        if (!local::xy.isInitialized) throw Error("Property 'xy' is not initialized")

        return local.xy
    }

    internal fun initializeAndReadXY(): String = doInitializeAndReadXY()

    private inline fun doInitializeAndReadOK(): String {
        if (::o.isInitialized) throw Error("Property 'o' already initialized")
        o = "O"
        if (!::o.isInitialized) throw Error("Property 'o' is not initialized")

        object {
            fun run() {
                if (::k.isInitialized) throw Error("Property 'k' already initialized")
                k = "K"
                if (!::k.isInitialized) throw Error("Property 'k' is not initialized")
            }
        }.run()

        val local = object {
            lateinit var ok: String
        }

        if (local::ok.isInitialized) throw Error("Property 'ok' already initialized")
        local.ok = o + k
        if (!local::ok.isInitialized) throw Error("Property 'ok' is not initialized")

        return local.ok
    }

    internal inline fun initializeAndReadOK(): String = doInitializeAndReadOK()
}

// MODULE: main()(lib)
// FILE: main.kt
fun box(): String = OK().initializeAndReadOK()
