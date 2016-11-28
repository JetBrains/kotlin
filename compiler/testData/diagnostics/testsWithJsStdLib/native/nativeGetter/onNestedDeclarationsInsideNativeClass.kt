// !DIAGNOSTICS: -UNUSED_PARAMETER, -DEPRECATION

external class A {
    class B {
        class A {
            @nativeGetter
            fun get(a: String): Any? = null

            @nativeGetter
            fun take(a: Number): String? = null

            @nativeGetter
            fun foo(a: Double): String? = null

            companion object {
                @nativeGetter
                fun get(a: String): Any? = null

                @nativeGetter
                fun take(a: Number): String? = null

                @nativeGetter
                fun foo(a: Double): String? = null
            }
        }

        class B {
            <!WRONG_ANNOTATION_TARGET!>@nativeGetter<!>
            val foo = 0

            <!WRONG_ANNOTATION_TARGET!>@nativeGetter<!>
            object Obj1 {}

            companion object {
                <!WRONG_ANNOTATION_TARGET!>@nativeGetter<!>
                val foo = 0

                <!WRONG_ANNOTATION_TARGET!>@nativeGetter<!>
                object Obj2 {}
            }
        }

        class C {
            <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>@nativeGetter
            fun get(): Any?<!> = null

            @nativeGetter
            fun get(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: A<!>): Any? = null

            @nativeGetter
            fun <!NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE!>foo<!>(a: Int) {}

            @nativeGetter
            fun bar(a: String): <!NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE!>Int<!> = 0

            @nativeGetter
            fun baz(<!NATIVE_INDEXER_CAN_NOT_HAVE_DEFAULT_ARGUMENTS!>a: Number = 1.1<!>): Int? = 0
        }

        object obj {
            <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>@nativeGetter
            fun get(): Any?<!> = null

            @nativeGetter
            fun get(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: A<!>): Any? = null

            @nativeGetter
            fun <!NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE!>foo<!>(a: Int) {}

            @nativeGetter
            fun bar(a: String): <!NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE!>Int<!> = 0

            @nativeGetter
            fun baz(<!NATIVE_INDEXER_CAN_NOT_HAVE_DEFAULT_ARGUMENTS!>a: String = "foo"<!>): Int? = 0
        }

        val anonymous = object {
            <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>@nativeGetter
            fun get(): Any?<!> = null

            @nativeGetter
            fun get(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: A<!>): Any? = null

            @nativeGetter
            fun <!NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE!>foo<!>(a: Int) {}

            @nativeGetter
            fun bar(a: String): <!NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE!>Int<!> = 0

            @nativeGetter
            fun baz(<!NATIVE_INDEXER_CAN_NOT_HAVE_DEFAULT_ARGUMENTS!>a: String = "foo"<!>): Int? = 0
        }
    }
}