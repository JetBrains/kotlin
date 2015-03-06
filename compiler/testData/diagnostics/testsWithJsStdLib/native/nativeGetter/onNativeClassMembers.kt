// !DIAGNOSTICS: -UNUSED_PARAMETER

native
class A {
    nativeGetter
    fun get(a: String): Any? = null

    nativeGetter
    fun take(a: Number): String? = null

    nativeGetter
    fun foo(a: Double): String? = null

    default object {
        nativeGetter
        fun get(a: String): Any? = null

        nativeGetter
        fun take(a: Number): String? = null

        nativeGetter
        fun foo(a: Double): String? = null
    }
}

native
class B {
    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
    val foo<!> = 0

    nativeGetter
    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>object Obj1<!> {}

    default object {
        <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
        val foo<!> = 0

        nativeGetter
        <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>object Obj2<!> {}
    }
}

native
class C {
    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
    fun Int.get(a: String): Int?<!> = 1

    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
    fun Int.get2(a: Number): String?<!> = "OK"

    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
    fun Int.get3(a: Int): String?<!> = "OK"

    <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>nativeGetter
    fun get(): Any?<!> = null

    nativeGetter
    fun get(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: A<!>): Any? = null

    nativeGetter
    fun <!NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE!>foo<!>(a: Int) {}

    nativeGetter
    fun bar(a: String): <!NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE!>Int<!> = 0

    nativeGetter
    fun baz(<!NATIVE_INDEXER_CAN_NOT_HAVE_DEFAULT_ARGUMENTS!>a: String = "foo"<!>): Int? = 0

    default object {
        <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
        fun Int.get(a: String): Int?<!> = 1

        <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
        fun Int.get2(a: Number): String?<!> = "OK"

        <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
        fun Int.get3(a: Int): String?<!> = "OK"
    }

}