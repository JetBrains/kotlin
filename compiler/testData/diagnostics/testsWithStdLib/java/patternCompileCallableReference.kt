// FIR_IDENTICAL
// FULL_JDK
// SKIP_TXT

import java.util.regex.Pattern

val strs: List<String> = listOf("regex1", "regex2")

val patterns: List<Pattern> = strs.map(Pattern::compile)
