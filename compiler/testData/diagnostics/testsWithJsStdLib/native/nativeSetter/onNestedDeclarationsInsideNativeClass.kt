// !DIAGNOSTICS: -UNUSED_PARAMETER

native
class A {
    class B {
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

        object obj {
            <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>nativeSetter
            fun set(): Any?<!> = null

            <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>nativeSetter
            fun set(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: A<!>): Any?<!> = null

            <!NATIVE_INDEXER_WRONG_PARAMETER_COUNT!>nativeSetter
            fun set(a: String, v: Any, v2: Any)<!> {}

            nativeSetter
            fun set(<!NATIVE_INDEXER_KEY_SHOULD_BE_STRING_OR_NUMBER!>a: A<!>, v: Any?) {}
        }

        val anonymous = object {
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
}