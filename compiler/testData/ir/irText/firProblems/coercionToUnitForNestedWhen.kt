// TARGET_BACKEND: JVM
// WITH_STDLIB
// FULL_JDK

// MUTE_SIGNATURE_COMPARISON_K2: ANY
// ^ KT-57778

import java.io.Reader
import java.io.StringReader

private const val BACKSLASH = '\\'

private fun Reader.nextChar(): Char? =
    read().takeUnless { it == -1 }?.toChar()

fun Reader.consumeRestOfQuotedSequence(sb: StringBuilder, quote: Char) {
    var ch = nextChar()
    while (ch != null && ch != quote) {
        if (ch == BACKSLASH) nextChar()?.let { sb.append(it) } else sb.append(ch)
        ch = nextChar()
    }
}
