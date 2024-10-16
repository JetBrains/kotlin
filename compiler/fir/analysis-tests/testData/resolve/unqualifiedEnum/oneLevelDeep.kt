// LANGUAGE: +ExpectedTypeGuidedResolution

enum class Day {
    MONDAY,
    TUESDAY;
}

fun test(x: Day): Int = if (x == <!UNRESOLVED_REFERENCE!>_.entries<!>.get(0)) 1 else 2
fun test2(x: Day): Int = if (x == _.valueOf("MONDAY")) 1 else 2
fun test3(x: Day): Int = if (x == <!UNRESOLVED_REFERENCE!>_.values<!>().get(0)) 1 else 2