// !DIAGNOSTICS: -UNUSED_PARAMETER, -DEPRECATION

fun foo() {
    @nativeGetter
    fun Int.get(a: String): Int? = 1

    @nativeGetter
    fun Int.get2(a: Number): String? = "OK"

    @nativeGetter
    fun Int.get3(a: Int): String? = "OK"

    @nativeGetter
    fun Int.get(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: Any<!>): Int? = 1

    <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>@nativeGetter
    fun Int.get2(): String?<!> = "OK"

    <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>@nativeGetter
    fun Int.get3(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: Any<!>, b: Int, c: Any?): String?<!> = "OK"

    @nativeGetter
    fun Any.foo(<!NATIVE_INDEXER_CAN_NOT_HAVE_DEFAULT_ARGUMENTS!>a: Int = 1<!>): Any? = "OK"
}