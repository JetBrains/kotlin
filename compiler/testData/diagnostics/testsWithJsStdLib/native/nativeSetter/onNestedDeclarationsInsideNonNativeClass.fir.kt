// !DIAGNOSTICS: -UNUSED_PARAMETER -NON_TOPLEVEL_CLASS_DECLARATION, -DEPRECATION

class A {
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
            fun Int.set(a: String, v: Int) {}

            @nativeSetter
            fun Int.set2(a: Number, v: String?) = "OK"

            @nativeSetter
            fun Int.set3(a: Double, v: String?) = "OK"

            @nativeSetter
            fun Int.set6(a: Double, v: String): Number = 1

            @nativeSetter
            fun set(): Any? = null

            @nativeSetter
            fun set(a: A): Any? = null

            @nativeSetter
            fun set(a: String, v: Any, v2: Any) {}

            @nativeSetter
            fun set(a: A, v: Any?) {}

            @nativeSetter
            fun foo(a: Double = 0.0, v: String = "str") = "OK"
        }

        object obj {
            @nativeSetter
            fun Int.set(a: String, v: Int) {}

            @nativeSetter
            fun Int.set2(a: Number, v: String?) = "OK"

            @nativeSetter
            fun Int.set3(a: Double, v: String?) = "OK"

            @nativeSetter
            fun set(): Any? = null

            @nativeSetter
            fun set(a: A): Any? = null

            @nativeSetter
            fun set(a: String, v: Any, v2: Any) {}

            @nativeSetter
            fun set(a: A, v: Any?) {}

            @nativeSetter
            fun foo(a: Int, v: String = "str") = "OK"
        }

        val anonymous = object {
            @nativeSetter
            fun Int.set(a: String, v: Int) {}

            @nativeSetter
            fun Int.set2(a: Number, v: String?) = "OK"

            @nativeSetter
            fun Int.set3(a: Double, v: String?) = "OK"

            @nativeSetter
            fun set(): Any? = null

            @nativeSetter
            fun set(a: A): Any? = null

            @nativeSetter
            fun set(a: String, v: Any, v2: Any) {}

            @nativeSetter
            fun set(a: A, v: Any?) {}

            @nativeSetter
            fun foo(a: Number = 0.0, v: String) = "OK"
        }
    }
}
