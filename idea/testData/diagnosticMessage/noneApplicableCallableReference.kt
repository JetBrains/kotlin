// !DIAGNOSTICS_NUMBER: 1
// !DIAGNOSTICS: NONE_APPLICABLE
// !LANGUAGE: -NewInference

fun filterChars(chars: List<Char>, str: String) =
        chars.filter(str::contains)
