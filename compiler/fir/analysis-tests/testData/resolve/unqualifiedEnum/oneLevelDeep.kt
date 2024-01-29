// LANGUAGE: +ExpectedTypeGuidedResolution

enum class Day {
    MONDAY,
    TUESDAY;
}

fun test(x: Day): Int = if (x == <!UNRESOLVED_REFERENCE!>entries<!>.get(0)) 1 else 2
fun test2(x: Day): Int = if (x == valueOf("MONDAY")) 1 else 2
fun test3(x: Day): Int = if (x == <!UNRESOLVED_REFERENCE!>values<!>().get(0)) 1 else 2