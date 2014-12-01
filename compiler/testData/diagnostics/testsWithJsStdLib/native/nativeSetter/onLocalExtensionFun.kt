// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo() {
    [nativeSetter]
    fun Int.set(a: String, v: Int) {}

    [nativeSetter]
    fun Int.set2(a: Number, v: String?) = "OK"

    [nativeSetter]
    fun Int.set3(a: Double, v: String?) = "OK"

    <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>[nativeSetter]
    fun Int.set(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: Any<!>): Int?<!> = 1

    <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>[nativeSetter]
    fun Int.set2(): String?<!> = "OK"

    <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>[nativeSetter]
    fun Int.set3(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: Any<!>, b: Int, c: Any?)<!> {}

    [nativeSetter]
    fun Any.foo(<!NATIVE_INDEXER_CAN_NOT_HAVE_DEFAULT_ARGUMENTS!>a: Double = 0.0<!>, <!NATIVE_INDEXER_CAN_NOT_HAVE_DEFAULT_ARGUMENTS!>v: String? = null<!>) = "OK"
}