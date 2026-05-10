// LANGUAGE: +CompanionBlocksAndExtensions
// DUMP_KLIB_ABI: DEFAULT
// WITH_STDLIB
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class C {
    companion {
        lateinit var o: String

        fun isInitialized() = ::o.isInitialized
    }
}

companion lateinit var C.k: String

fun box(): String {
    assertFalse(C.isInitialized())
    assertFalse(C::k.isInitialized)

    C.o = "O"
    C.k = "K"

    assertTrue(C.isInitialized())
    assertTrue(C::k.isInitialized)

    return C.o + C.k
}
