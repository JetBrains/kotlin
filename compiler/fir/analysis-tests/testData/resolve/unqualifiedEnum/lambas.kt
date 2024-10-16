// LANGUAGE: +ExpectedTypeGuidedResolution

package test

enum class Sample {
    FIRST, SECOND, THIRD;
}

val foo: () -> Sample = { _.FIRST }

fun bar(n: Int): Sample = n.let { <!UNRESOLVED_REFERENCE!>_.FIRST<!> }