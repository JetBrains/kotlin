// !DIAGNOSTICS: -UNUSED_PARAMETER

native
class A {
    class B {
        class A {
            nativeGetter
            fun get(a: String): Any? = null

            nativeGetter
            fun take(a: Number): String? = null

            nativeGetter
            fun foo(a: Double): String? = null

            class object {
                nativeGetter
                fun get(a: String): Any? = null

                nativeGetter
                fun take(a: Number): String? = null

                nativeGetter
                fun foo(a: Double): String? = null
            }
        }

        class B {
            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
            val foo<!> = 0

            nativeGetter
            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>object Obj1<!> {}

            class object {
                <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
                val foo<!> = 0

                nativeGetter
                <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>object Obj2<!> {}
            }
        }

        class C {
            <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>nativeGetter
            fun get(): Any?<!> = null

            nativeGetter
            fun get(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: A<!>): Any? = null

            nativeGetter
            fun <!NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE!>foo<!>(a: Int) {}

            nativeGetter
            fun bar(a: String): <!NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE!>Int<!> = 0
        }

        object obj {
            <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>nativeGetter
            fun get(): Any?<!> = null

            nativeGetter
            fun get(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: A<!>): Any? = null

            nativeGetter
            fun <!NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE!>foo<!>(a: Int) {}

            nativeGetter
            fun bar(a: String): <!NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE!>Int<!> = 0
        }

        val anonymous = object {
            <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>nativeGetter
            fun get(): Any?<!> = null

            nativeGetter
            fun get(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: A<!>): Any? = null

            nativeGetter
            fun <!NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE!>foo<!>(a: Int) {}

            nativeGetter
            fun bar(a: String): <!NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE!>Int<!> = 0
        }
    }
}