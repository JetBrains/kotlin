// !LANGUAGE: -NestedClassesInEnumEntryShouldBeInner

enum class E {
    ENTRY,
    SUBCLASS {
        object O {
            fun foo() = 2
        }
        override fun bar() = O.foo()
    };

    object O {
        fun foo() = 1
    }
    open fun bar() = O.foo()
}

fun box(): String {
    if (E.ENTRY.bar() != 1) return "Fail 1"
    if (E.SUBCLASS.bar() != 2) return "Fail 2"
    return "OK"
}
