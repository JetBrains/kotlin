// !DIAGNOSTICS: -UNUSED_PARAMETER

fun foo() {
    [native]
    class A {
        nativeSetter
        fun set(a: String, v: Any?): Any? = null

        nativeSetter
        fun put(a: Number, v: String) {}

        nativeSetter
        fun foo(a: Int, v: String) {}
    }

    [native]
    class B {
        <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeSetter
        val foo<!> = 0
    }

    [native]
    class C {
        <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>nativeSetter
        fun set(): Any?<!> = null

        <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>nativeSetter
        fun set(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: A<!>): Any?<!> = null

        <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>nativeSetter
        fun set(a: String, v: Any, v2: Any)<!> {}

        nativeSetter
        fun set(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: A<!>, v: Any?) {}
    }
}