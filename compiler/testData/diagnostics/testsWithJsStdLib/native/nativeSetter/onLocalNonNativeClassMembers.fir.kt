// !DIAGNOSTICS: -UNUSED_PARAMETER -NON_TOPLEVEL_CLASS_DECLARATION, -DEPRECATION

fun foo() {
    class A {
        @nativeSetter
        fun set(a: String, v: Any?): Any? = null

        @nativeSetter
        fun put(a: Number, v: String) {}

        @nativeSetter
        fun foo(a: Int, v: String) {}
    }

    class B {
        <!WRONG_ANNOTATION_TARGET!>@nativeSetter<!>
        var foo = 0
    }

    class C {
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
        fun foo(a: Int = 0, v: String) = "OK"
    }
}
