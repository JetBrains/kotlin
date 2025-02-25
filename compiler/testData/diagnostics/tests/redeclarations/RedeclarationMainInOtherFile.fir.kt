// RUN_PIPELINE_TILL: FRONTEND
// LANGUAGE: +ContextParameters
// DIAGNOSTICS: -UNUSED_PARAMETER

// FILE: fake_main1.kt
<!CONFLICTING_OVERLOADS!>fun main(): (Array<String>) -> Unit<!> = { args: Array<String> -> Unit }

<!CONFLICTING_OVERLOADS!>fun nonmain(): (Array<String>) -> Unit<!> = { args: Array<String> -> Unit }

context(c: Array<String>) <!CONFLICTING_OVERLOADS!>fun main()<!> {}

// FILE: fake_main2.kt
<!CONFLICTING_OVERLOADS!>fun main(): Any<!> = ""

<!CONFLICTING_OVERLOADS!>fun nonmain(): Any<!> = ""

context(c: Array<String>) <!CONFLICTING_OVERLOADS!>fun main()<!> {}

// FILE: real_main.kt
<!CONFLICTING_OVERLOADS!>fun main()<!> {}

<!CONFLICTING_OVERLOADS!>fun nonmain()<!> {}
