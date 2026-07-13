// WITH_STDLIB
// LANGUAGE: -IntrinsicConstEvaluation

annotation class Anno(val value: String, val number: Int)

@Anno("value".uppercase(), 60 * 60)
fun fo<caret>o() {}