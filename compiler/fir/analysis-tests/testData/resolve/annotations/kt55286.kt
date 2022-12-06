// ISSUE: KT-55286

annotation class Deprecated<T>

open class Base(
    @Deprecated<<!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>Nested<!>> val a: String,
) {
    class Nested
}

class Derived(
    @Deprecated<<!UNRESOLVED_REFERENCE, UNRESOLVED_REFERENCE!>Nested<!>> val b: String,
) : Base("")
