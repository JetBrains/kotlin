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
            @nativeGetter
            fun get(): Any? = definedExternally

            @nativeGetter
            fun get(a: A): Any? = definedExternally

            @nativeGetter
            fun foo(a: Int) { definedExternally }

            @nativeGetter
            fun bar(a: String): Int = definedExternally

            @nativeGetter
            fun baz(a: Number = definedExternally): Int? = definedExternally
        }

        object obj {
            @nativeGetter
            fun get(): Any? = definedExternally

            @nativeGetter
            fun get(a: A): Any? = definedExternally

            @nativeGetter
            fun foo(a: Int) { definedExternally }

            @nativeGetter
            fun bar(a: String): Int = definedExternally

            @nativeGetter
            fun baz(a: String = definedExternally): Int? = definedExternally
        }
    }
}
