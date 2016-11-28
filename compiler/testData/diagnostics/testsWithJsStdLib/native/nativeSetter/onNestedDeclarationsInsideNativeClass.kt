// !DIAGNOSTICS: -UNUSED_PARAMETER, -DEPRECATION

external class A {
    class B {
        class A {
            @nativeSetter
            fun set(a: String, v: Any?): Any? = null

            @nativeSetter
            fun put(a: Number, v: String) {}

            @nativeSetter
            fun foo(a: Int, v: String) {}

            @nativeSetter
            fun set4(a: Double, v: String): Any = 1

            @nativeSetter
            fun set5(a: Double, v: String): CharSequence = "OK"

            companion object {
                @nativeSetter
                fun set(a: String, v: Any?): Any? = null

                @nativeSetter
                fun put(a: Number, v: String) {}

                @nativeSetter
                fun foo(a: Int, v: String) {}

                @nativeSetter
                fun set4(a: Double, v: String): Any = 1

                @nativeSetter
                fun set5(a: Double, v: String): CharSequence = "OK"
            }
        }

        class B {
            <!WRONG_ANNOTATION_TARGET!>@nativeSetter<!>
            val foo = 0

            <!WRONG_ANNOTATION_TARGET!>@nativeSetter<!>
            object Obj1 {}

            companion object {
                <!WRONG_ANNOTATION_TARGET!>@nativeSetter<!>
                val foo = 0

                <!WRONG_ANNOTATION_TARGET!>@nativeSetter<!>
                object Obj2 {}
            }
        }

        class C {
            @nativeSetter
            fun set6(a: Double, v: String): <!NATIVE_SETTER_WRONG_RETURN_TYPE!>Number<!> = 1

            <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>@nativeSetter
            fun set(): Any?<!> = null

            <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>@nativeSetter
            fun set(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: A<!>): Any?<!> = null

            <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>@nativeSetter
            fun set(a: String, v: Any, v2: Any)<!> {}

            @nativeSetter
            fun set(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: A<!>, v: Any?) {}

            @nativeSetter
            fun foo(<!NATIVE_INDEXER_CAN_NOT_HAVE_DEFAULT_ARGUMENTS!>a: Double = 0.0<!>, <!NATIVE_INDEXER_CAN_NOT_HAVE_DEFAULT_ARGUMENTS!>v: String = "str"<!>) = "OK"
        }

        object obj {
            <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>@nativeSetter
            fun set(): Any?<!> = null

            <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>@nativeSetter
            fun set(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: A<!>): Any?<!> = null

            <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>@nativeSetter
            fun set(a: String, v: Any, v2: Any)<!> {}

            @nativeSetter
            fun set(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: A<!>, v: Any?) {}

            @nativeSetter
            fun foo(a: Int, <!NATIVE_INDEXER_CAN_NOT_HAVE_DEFAULT_ARGUMENTS!>v: String = "str"<!>) = "OK"
        }

        val anonymous = object {
            <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>@nativeSetter
            fun set(): Any?<!> = null

            <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>@nativeSetter
            fun set(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: A<!>): Any?<!> = null

            <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>@nativeSetter
            fun set(a: String, v: Any, v2: Any)<!> {}

            @nativeSetter
            fun set(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: A<!>, v: Any?) {}

            @nativeSetter
            fun foo(<!NATIVE_INDEXER_CAN_NOT_HAVE_DEFAULT_ARGUMENTS!>a: Number = 0.0<!>, v: String) = "OK"
        }
    }
}