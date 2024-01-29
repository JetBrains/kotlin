// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ExpectedTypeGuidedResolution

package test

enum class Sample {
    FIRST, SECOND, THIRD;
}

val foo: () -> Sample = { FIRST }

fun boo(): Sample = FIRST

fun <A, B> A.let(transform: (A) -> B): B = transform(this)
fun <A> A.letSample(transform: (A) -> Sample): Sample = transform(this)

fun bar1(n: Int): Sample = n.let { <!UNRESOLVED_REFERENCE!>FIRST<!> }
fun bar2(n: Int): Sample = n.letSample { FIRST }