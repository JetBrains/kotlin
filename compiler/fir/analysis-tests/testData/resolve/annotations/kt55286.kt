// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-55286
// LATEST_LV_DIFFERENCE

annotation class Deprecated<T>

open class Base(
    @Deprecated<Nested> val a: String,
) {
    class Nested
}

class Derived(
    @Deprecated<Nested> val b: String,
) : Base("")
