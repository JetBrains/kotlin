// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

header class OuterClass {
    header class NestedClass {
        header class DeepNested {
            header class Another
        }
    }

    header inner class InnerClass

    header companion object
}

header class OuterClassWithNamedCompanion {
    header companion object Factory
}

header object OuterObject {
    header object NestedObject
}

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt

impl class OuterClass {
    impl class NestedClass {
        impl class DeepNested {
            impl class Another
        }
    }

    impl inner class InnerClass

    impl companion object
}

impl class OuterClassWithNamedCompanion {
    impl companion object Factory
}

impl object OuterObject {
    impl object NestedObject
}
