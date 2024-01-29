// LANGUAGE: +ExpectedTypeGuidedResolution

package test

enum class Sample {
    FIRST, SECOND, THIRD;
}

val foo: () -> Sample = { FIRST }

fun bar(n: Int): Sample = n.let { <!UNRESOLVED_REFERENCE!>FIRST<!> }