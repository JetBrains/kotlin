// SKIP_TXT

class Foo<T> {
    companion object {
        fun foo(arg: <!UNRESOLVED_REFERENCE!>T<!>) {}
    }

    object O {
        fun foo(arg: <!UNRESOLVED_REFERENCE!>T<!>) {}
    }

    class Nested {
        fun foo(arg: <!UNRESOLVED_REFERENCE!>T<!>) {}
    }

    inner class Inner<R> {
        fun foo(arg1: T, arg2: R) {}

        <!NESTED_CLASS_NOT_ALLOWED!>class InnerNested<!> {
            fun foo(arg1: <!INACCESSIBLE_OUTER_CLASS_EXPRESSION!>T<!>, arg2: <!UNRESOLVED_REFERENCE!>R<!>) {}
        }
    }

    enum class E {
        ;

        fun foo(arg: <!UNRESOLVED_REFERENCE!>T<!>) {}
    }

    val obj = object {
        fun foo(arg: T) {}
    }

    fun <R> bar() {
        class Local {
            fun baz(arg1: T, arg2: R) {}
        }
    }
}
