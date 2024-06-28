// SKIP_WHEN_OUT_OF_CONTENT_ROOT
// MODULE: m1
// FILE: AbstractClass.kt
abstract class AbstractClass<T> {
    abstract val property: T
}

// MODULE: m2(m1)
// MEMBER_NAME_FILTER: property
// FILE: SubClass.kt
abstract class S<caret>ubClass: AbstractClass<Int>()
