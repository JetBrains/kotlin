// ISSUE: KT-55286

annotation class Deprecated<T>

open class Base(
    @Deprecated<Nested> val a: String,
) {
    class Nested
}

class Derived(
    @Deprecated<Nested> val b: String,
) : Base("")
