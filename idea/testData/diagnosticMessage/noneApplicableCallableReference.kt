// !DIAGNOSTICS_NUMBER: 1
// !DIAGNOSTICS: NONE_APPLICABLE

fun filterChars(chars: List<Char>, str: String) =
        chars.filter(str::contains)
