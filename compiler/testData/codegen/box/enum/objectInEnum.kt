// !LANGUAGE: -NestedClassesInEnumEntryShouldBeInner
// IGNORE_BACKEND: NATIVE

enum class E {
    ENTRY,
    SUBCLASS {
        // Because of KT-45115 classes/objects inside enum entries are local in FIR
        @Suppress("LOCAL_OBJECT_NOT_ALLOWED")
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
