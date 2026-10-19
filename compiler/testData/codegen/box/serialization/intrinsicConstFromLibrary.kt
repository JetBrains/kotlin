// WITH_STDLIB

// MODULE: lib
// FILE: lib.kt
package constants

const val text = " o ".trim().uppercase()
const val letter = Char(75)
const val count = 40.inc()
const val signed = 4294967296L.inc()
const val unsigned = 4294967296UL.inc()
const val narrowed = 65536u.toUShort()

// MODULE: main(lib)
// FILE: main.kt
import constants.*

const val consumerText = text + letter
const val consumerCount = count.inc()
const val consumerSigned = signed.inc()
const val consumerUnsigned = unsigned.inc()

fun <T> T.id(): T = this

fun box(): String {
    if (text.id() != "O" || letter.id() != 'K') return "Fail library text"
    if (consumerText.id() != "OK") return "Fail consumer text"
    if (consumerCount.id() != 42) return "Fail consumer count"
    if (signed.id() != 4294967297L || consumerSigned.id() != 4294967298L) return "Fail signed"
    if (unsigned.id() != 4294967297UL || consumerUnsigned.id() != 4294967298UL) return "Fail unsigned"
    if (narrowed.id() != 0u.toUShort()) return "Fail narrowed"
    return "OK"
}
