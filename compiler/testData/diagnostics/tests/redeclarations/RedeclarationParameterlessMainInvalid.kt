// FILE: a.kt
<!CONFLICTING_OVERLOADS!>fun main()<!> {}

suspend fun main(args: Array<String>) {}

// FILE: b.kt
<!CONFLICTING_OVERLOADS!>fun main()<!> {}
