// !LANGUAGE: -VariableDeclarationInWhenSubject
// !DIAGNOSTICS: -UNUSED_VARIABLE

fun foo(): Any = 42

fun test(x: Any) {
    // NB check that we still resolve 'y', even though current language version doesn' support variable declaration in when subject

    val z1 = when (<!UNSUPPORTED_FEATURE!>val y = foo()<!>) {
        42 -> "Magic: $y, $x"
        else -> {
            "Not magic: $y, $x"
        }
    }
    val z2 = "Anyway, it was $<!UNRESOLVED_REFERENCE!>y<!>"
}