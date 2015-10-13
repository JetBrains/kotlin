// !DIAGNOSTICS: -UNUSED_PARAMETER

<!NOTHING_TO_INLINE!>inline<!> fun foo(<!ILLEGAL_INLINE_PARAMETER_MODIFIER!>noinline<!> x: Int) {}

<!NOTHING_TO_INLINE!>inline<!> fun bar(y: Int, <!ILLEGAL_INLINE_PARAMETER_MODIFIER!>crossinline<!> x: String) {}

fun gav(<!ILLEGAL_INLINE_PARAMETER_MODIFIER!>noinline<!> x: (Int) -> Unit, <!ILLEGAL_INLINE_PARAMETER_MODIFIER!>crossinline<!> y: (String) -> Int) {}

inline fun correct(noinline x: (Int) -> Unit, crossinline y: (String) -> Int) {}

