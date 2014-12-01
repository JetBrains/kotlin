// !DIAGNOSTICS: -UNUSED_PARAMETER

native
class A {
    nativeSetter
    fun set(a: String, v: Any?): Any? = null

    nativeSetter
    fun put(a: Number, v: String) {}

    nativeSetter
    fun foo(a: Int, v: String) {}

    class object {
        nativeSetter
        fun set(a: String, v: Any?): Any? = null

        nativeSetter
        fun put(a: Number, v: String) {}

        nativeSetter
        fun foo(a: Int, v: String) {}
    }
}

native
class B {
    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeSetter
    val foo<!> = 0

    nativeSetter
    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>object Obj1<!> {}

    class object {
        <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeSetter
        val foo<!> = 0

        nativeSetter
        <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>object Obj2<!> {}
    }
}

native
class C {
    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeSetter
    fun Int.set(a: String, v: Int)<!> {}

    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeSetter
    fun Int.set2(a: Number, v: String?)<!> = "OK"

    <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeSetter
    fun Int.set3(a: Double, v: String?)<!> = "OK"

    <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>nativeSetter
    fun set(): Any?<!> = null

    <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>nativeSetter
    fun set(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: A<!>): Any?<!> = null

    <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>nativeSetter
    fun set(a: String, v: Any, v2: Any)<!> {}

    nativeSetter
    fun set(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: A<!>, v: Any?) {}

    [nativeSetter]
    fun foo(a: Number, <!NATIVE_INDEXER_CAN_NOT_HAVE_DEFAULT_ARGUMENTS!>v: String = "aa"<!>) = "OK"
}