import kotlin.text.*

const val trimmed = "  1  ".<!EVALUATED: `1`!>trim()<!>
const val trimmedWithPredicate = ("  2  " as CharSequence).trim { it.isWhitespace() }.<!EVALUATED: `2`!>toString()<!>
const val charSequenceTrim = ("  3  " as CharSequence).trim().<!EVALUATED: `3`!>toString()<!>
