// Escape sequences have to be split into separate string template entries
// FILE: Escaped.kt
annotation class Escaped(
    val s: String,
    val c: Char,
)

// FILE: WithControlCharacters.kt
@Escaped("\n\t\r\b", '\n')
class WithControlCharacters

// FILE: WithQuotes.kt
@Escaped("\"quoted\"", '\'')
class WithQuotes

// FILE: WithBackslash.kt
@Escaped("back\\slash", '\\')
class WithBackslash

// FILE: WithNonPrintableCharacters.kt
@Escaped("\u0001nonPrintable\u007F", '\u0001')
class WithNonPrintableCharacters

// FILE: WithDollar.kt
@Escaped("\$notATemplate\${}", '$')
class WithDollar

// FILE: WithMixedContent.kt
@Escaped("prefix\nsuffix", ' ')
class WithMixedContent
