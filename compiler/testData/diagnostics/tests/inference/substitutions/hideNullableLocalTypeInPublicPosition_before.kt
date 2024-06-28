// ISSUE: KT-30054
// LANGUAGE: -KeepNullabilityWhenApproximatingLocalType
interface I {
    fun foo(): String
}

<!APPROXIMATED_LOCAL_TYPE_WILL_BECOME_NULLABLE!>fun bar(condition: Boolean)<!> /*: I? */ =
    if (condition)
        object : I {
            override fun foo() = "should check for null first"
            fun baz() = "invisible"
        }
    else null

fun main() {
    bar(false).<!UNRESOLVED_REFERENCE!>baz<!>()
    bar(false).foo()
    bar(false)<!UNNECESSARY_SAFE_CALL!>?.<!>foo()
}
