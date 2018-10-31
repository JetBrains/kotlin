// FILE: a.kt
<!CONFLICTING_OVERLOADS!>fun main()<!> {}

suspend fun main(<!UNUSED_PARAMETER!>args<!>: Array<String>) {}

// FILE: b.kt
<!CONFLICTING_OVERLOADS!>fun main()<!> {}
