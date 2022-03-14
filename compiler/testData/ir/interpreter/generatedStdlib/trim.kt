import kotlin.text.*

const val trimmed = <!EVALUATED: `1`!>"  1  ".trim()<!>
const val trimmedWithPredicate = <!EVALUATED: `2`!>("  2  " as CharSequence).trim { it.isWhitespace() }.toString()<!>
const val charSequenceTrim = <!EVALUATED: `3`!>("  3  " as CharSequence).trim().toString()<!>
