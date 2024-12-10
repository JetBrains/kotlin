// ISSUE: KT-73801
// RUN_PIPELINE_TILL: BACKEND
suspend fun test(): String = "123"

val test: String = "456"

interface Base {
    suspend fun test(): String
}

interface Derived : Base {
    <!CONFLICTING_OVERLOADS!>var test: String<!>
}
