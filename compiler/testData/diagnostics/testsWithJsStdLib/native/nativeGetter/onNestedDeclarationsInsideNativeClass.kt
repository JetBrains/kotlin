// FIR_IDENTICAL
// !DIAGNOSTICS: -UNUSED_PARAMETER, -DEPRECATION

external class A {
    class B {
        class A {
            @nativeGetter
            fun get(a: String): Any? = definedExternally

            @nativeGetter
            fun take(a: Number): String? = definedExternally

            @nativeGetter
            fun foo(a: Double): String? = definedExternally

            companion object {
                @nativeGetter
                fun get(a: String): Any? = definedExternally

                @nativeGetter
                fun take(a: Number): String? = definedExternally

                @nativeGetter
                fun foo(a: Double): String? = definedExternally
            }
        }

        class B {
            <!WRONG_ANNOTATION_TARGET!>@nativeGetter<!>
            val foo: Int = definedExternally

            <!WRONG_ANNOTATION_TARGET!>@nativeGetter<!>
            object Obj1 {}

            companion object {
                <!WRONG_ANNOTATION_TARGET!>@nativeGetter<!>
                val foo: Int = definedExternally

                <!WRONG_ANNOTATION_TARGET!>@nativeGetter<!>
                object Obj2 {}
            }
        }

        class C {
            <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>@nativeGetter
            fun get(): Any?<!> = definedExternally

            @nativeGetter
            fun get(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: A<!>): Any? = definedExternally

            @nativeGetter
            fun <!NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE!>foo<!>(a: Int) { definedExternally }

            @nativeGetter
            fun bar(a: String): <!NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE!>Int<!> = definedExternally

            @nativeGetter
            fun baz(<!NATIVE_INDEXER_CAN_NOT_HAVE_DEFAULT_ARGUMENTS!>a: Number = definedExternally<!>): Int? = definedExternally
        }

        object obj {
            <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>@nativeGetter
            fun get(): Any?<!> = definedExternally

            @nativeGetter
            fun get(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: A<!>): Any? = definedExternally

            @nativeGetter
            fun <!NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE!>foo<!>(a: Int) { definedExternally }

            @nativeGetter
            fun bar(a: String): <!NATIVE_GETTER_RETURN_TYPE_SHOULD_BE_NULLABLE!>Int<!> = definedExternally

            @nativeGetter
            fun baz(<!NATIVE_INDEXER_CAN_NOT_HAVE_DEFAULT_ARGUMENTS!>a: String = definedExternally<!>): Int? = definedExternally
        }
    }
}