// RUN_PIPELINE_TILL: BACKEND
// ISSUE: KT-55286

annotation class Deprecated<T>

open class Base(
    <!ANNOTATION_WILL_BE_APPLIED_ALSO_TO_PROPERTY_OR_FIELD("property")!>@Deprecated<Nested><!> val a: String,
) {
    class Nested
}

class Derived(
    <!ANNOTATION_WILL_BE_APPLIED_ALSO_TO_PROPERTY_OR_FIELD("property")!>@Deprecated<Nested><!> val b: String,
) : Base("")
