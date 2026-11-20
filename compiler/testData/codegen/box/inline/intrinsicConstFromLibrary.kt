// WITH_STDLIB

// MODULE: lib
// FILE: lib.kt
package constants

const val text = " ok ".trim().uppercase()
const val count = 41.inc()
const val letter = Char(75)
const val signed = 4294967296L.inc()
const val unsigned = 4294967296UL.inc()

inline fun consumeConstants(block: (String, Int, Char, Long, ULong) -> String): String =
    block(text, count, letter, signed, unsigned)

inline fun consumeDefault(value: String = text, block: (String) -> String): String = block(value)

// MODULE: bridge(lib)
// FILE: bridge.kt
package bridge

inline fun forwardConstants(block: (String, Int, Char, Long, ULong) -> String): String =
    constants.consumeConstants(block)

// MODULE: main(lib, bridge)
// FILE: main.kt
fun <T> T.id(): T = this

fun box(): String {
    val result = bridge.forwardConstants { text, count, letter, signed, unsigned ->
        if (text.id() != "OK") "Fail text"
        else if (count.id() != 42) "Fail count"
        else if (letter.id() != 'K') "Fail Char"
        else if (signed.id() != 4294967297L) "Fail Long"
        else if (unsigned.id() != 4294967297UL) "Fail ULong"
        else "OK"
    }
    if (result != "OK") return result
    return constants.consumeDefault { if (it.id() == "OK") "OK" else "Fail default" }
}
