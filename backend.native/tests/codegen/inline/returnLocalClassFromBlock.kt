package codegen.inline.returnLocalClassFromBlock

import kotlin.test.*

inline fun <R> call(block: ()->R): R {
    try {
        return block()
    } finally {
        println("Zzz")
    }
}

@Test fun runTest() {
    call { class Z(); Z() }
}
