// !DIAGNOSTICS: -UNUSED_PARAMETER, -DEPRECATION

external class A {
    class B {
        class A {
            @nativeGetter
            fun get(a: String): Any? = noImpl

            @nativeGetter
            fun take(a: Number): String? = noImpl

            @nativeGetter
            fun foo(a: Double): String? = noImpl

            companion object {
                @nativeGetter
                fun get(a: String): Any? = noImpl

                @nativeGetter
                fun take(a: Number): String? = noImpl

                @nativeGetter
                fun foo(a: Double): String? = noImpl
            }
        }

        class B {
            <!WRONG_ANNOTATION_TARGET!>@nativeGetter<!>
            val foo: Int = noImpl

            <!WRONG_ANNOTATION_TARGET!>@nativeGetter<!>
            object Obj1 {}

            companion object {
                <!WRONG_ANNOTATION_TARGET!>@nativeGetter<!>
                val foo: Int = noImpl

                <!WRONG_ANNOTATION_TARGET!>@nativeGetter<!>
                object Obj2 {}
            }
        }

        class C {
            <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>@nativeGetter
            fun get(): Any?<!> = noImpl

            @nativeGetter
            fun get(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: A<!>): Any? = noImpl

            @nativeGetter
            fun <!NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE!>foo<!>(a: Int) { noImpl }

            @nativeGetter
            fun bar(a: String): <!NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE!>Int<!> = noImpl

            @nativeGetter
            fun baz(<!NATIVE_INDEXER_CAN_NOT_HAVE_DEFAULT_ARGUMENTS!>a: Number = noImpl<!>): Int? = noImpl
        }

        object obj {
            <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>@nativeGetter
            fun get(): Any?<!> = noImpl

            @nativeGetter
            fun get(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: A<!>): Any? = noImpl

            @nativeGetter
            fun <!NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE!>foo<!>(a: Int) { noImpl }

            @nativeGetter
            fun bar(a: String): <!NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE!>Int<!> = noImpl

            @nativeGetter
            fun baz(<!NATIVE_INDEXER_CAN_NOT_HAVE_DEFAULT_ARGUMENTS!>a: String = noImpl<!>): Int? = noImpl
        }
    }
}