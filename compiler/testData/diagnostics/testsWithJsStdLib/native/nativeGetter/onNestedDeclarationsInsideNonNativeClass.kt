// !DIAGNOSTICS: -UNUSED_PARAMETER

class A {
    class B {
        class A {
            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
            fun get(a: String): Any?<!> = null

            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
            fun take(a: Number): String?<!> = null

            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
            fun foo(a: Double): String?<!> = null

            default object {
                <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
                fun get(a: String): Any?<!> = null

                <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
                fun take(a: Number): String?<!> = null

                <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
                fun foo(a: Double): String?<!> = null
            }
        }

        class B {
            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
            fun Int.get(a: String): Int?<!> = 1

            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
            fun Int.get2(a: Number): String?<!> = "OK"

            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
            fun Int.get3(a: Int): String?<!> = "OK"

            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
            val foo<!> = 0

            nativeGetter
            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>object Obj1<!> {}

            default object {
                <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
                val foo<!> = 0

                nativeGetter
                <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>object Obj2<!> {}

                <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
                fun Int.get(a: String): Int?<!> = 1

                <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
                fun Int.get2(a: Number): String?<!> = "OK"

                <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
                fun Int.get3(a: Int): String?<!> = "OK"
            }
        }

        class C {
            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN, NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>nativeGetter
            fun get(): Any?<!> = null

            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
            fun get(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: A<!>): Any?<!> = null

            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
            fun <!NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE!>foo<!>(a: Int)<!> {}

            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
            fun bar(a: String): <!NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE!>Int<!><!> = 0

            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
            fun baz(<!NATIVE_INDEXER_CAN_NOT_HAVE_DEFAULT_ARGUMENTS!>a: String = "foo"<!>): Int?<!> = 0
        }

        object obj {
            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN, NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>nativeGetter
            fun get(): Any?<!> = null

            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
            fun get(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: A<!>): Any?<!> = null

            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
            fun <!NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE!>foo<!>(a: Int)<!> {}

            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
            fun bar(a: String): <!NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE!>Int<!><!> = 0

            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
            fun baz(<!NATIVE_INDEXER_CAN_NOT_HAVE_DEFAULT_ARGUMENTS!>a: Double = 0.0<!>): Int?<!> = 0
        }

        val anonymous = object {
            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN, NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>nativeGetter
            fun get(): Any?<!> = null

            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
            fun get(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: A<!>): Any?<!> = null

            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
            fun <!NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE!>foo<!>(a: Int)<!> {}

            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
            fun bar(a: String): <!NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE!>Int<!><!> = 0

            <!NATIVE_ANNOTATIONS_ALLOWED_ONLY_ON_MEMBER_OR_EXTENSION_FUN!>nativeGetter
            fun baz(<!NATIVE_INDEXER_CAN_NOT_HAVE_DEFAULT_ARGUMENTS!>a: String = "foo"<!>): Int?<!> = 0
        }
    }
}