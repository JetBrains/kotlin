// RUN_PIPELINE_TILL: FRONTEND
// DIAGNOSTICS: -UNUSED_PARAMETER, -DEPRECATION

fun foo() {
    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_FUN!>@nativeGetter<!>
    fun Int.get(a: String): Int? = 1

    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_FUN!>@nativeGetter<!>
    fun Int.get2(a: Number): String? = "OK"

    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_FUN!>@nativeGetter<!>
    fun Int.get3(a: Int): String? = "OK"

    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_FUN!>@nativeGetter<!>
    fun Int.get(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: Any<!>): Int? = 1

    <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!><!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_FUN!>@nativeGetter<!>
    fun Int.get2(): String?<!> = "OK"

    <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!><!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_FUN!>@nativeGetter<!>
    fun Int.get3(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: Any<!>, b: Int, c: Any?): String?<!> = "OK"

    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_FUN!>@nativeGetter<!>
    fun Any.foo(<!NATIVE_INDEXER_CAN_NOT_HAVE_DEFAULT_ARGUMENTS!>a: Int = 1<!>): Any? = "OK"
}