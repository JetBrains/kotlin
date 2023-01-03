// !DIAGNOSTICS: -UNUSED_PARAMETER -NON_TOPLEVEL_CLASS_DECLARATION, -DEPRECATION

class A {
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
            @nativeGetter
            fun Int.get(a: String): Int? = 1

            @nativeGetter
            fun Int.get2(a: Number): String? = "OK"

            @nativeGetter
            fun Int.get3(a: Int): String? = "OK"

            <!WRONG_ANNOTATION_TARGET!>@nativeGetter<!>
            val foo = 0

            <!WRONG_ANNOTATION_TARGET!>@nativeGetter<!>
            object Obj1 {}

            companion object {
                <!WRONG_ANNOTATION_TARGET!>@nativeGetter<!>
                val foo = 0

                <!WRONG_ANNOTATION_TARGET!>@nativeGetter<!>
                object Obj2 {}

                @nativeGetter
                fun Int.get(a: String): Int? = 1

                @nativeGetter
                fun Int.get2(a: Number): String? = "OK"

                @nativeGetter
                fun Int.get3(a: Int): String? = "OK"
            }
        }

        class C {
            @nativeGetter
            fun get(): Any? = null

            @nativeGetter
            fun get(a: A): Any? = null

            @nativeGetter
            fun foo(a: Int) {}

            @nativeGetter
            fun bar(a: String): Int = 0

            @nativeGetter
            fun baz(a: String = "foo"): Int? = 0
        }

        object obj {
            @nativeGetter
            fun get(): Any? = null

            @nativeGetter
            fun get(a: A): Any? = null

            @nativeGetter
            fun foo(a: Int) {}

            @nativeGetter
            fun bar(a: String): Int = 0

            @nativeGetter
            fun baz(a: Double = 0.0): Int? = 0
        }

        val anonymous = object {
            @nativeGetter
            fun get(): Any? = null

            @nativeGetter
            fun get(a: A): Any? = null

            @nativeGetter
            fun foo(a: Int) {}

            @nativeGetter
            fun bar(a: String): Int = 0

            @nativeGetter
            fun baz(a: String = "foo"): Int? = 0
        }
    }
}
