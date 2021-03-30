// !LANGUAGE: -ExtendedMainConvention

// FILE: a.kt
<!CONFLICTING_OVERLOADS!>suspend fun main(args: Array<String>)<!> {}

// FILE: b.kt
<!CONFLICTING_OVERLOADS!>suspend fun main(args: Array<String>)<!> {}
