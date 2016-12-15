// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

header class OuterClass {
    header class NestedClass

    header inner class InnerClass

    header companion object
}

header class OuterClassWithNamedCompanion {
    header companion object Factory
}

header object OuterObject {
    header object NestedObject
}

header class ByTypeAlias {
    header interface Nested
}

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt

impl class OuterClass {
    impl class NestedClass

    impl inner class InnerClass

    impl companion object
}

impl class OuterClassWithNamedCompanion {
    impl companion object Factory
}

impl object OuterObject {
    impl object NestedObject
}


class ByTypeAliasImpl {
    interface Nested
}

impl typealias ByTypeAlias = ByTypeAliasImpl
