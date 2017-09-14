// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

header class OuterClass {
    class NestedClass {
        class DeepNested {
            class Another {
                fun f(s: String)
                val p: Int
            }
        }
    }

    inner class InnerClass {
        fun f(x: Int)
        val p: String
    }

    companion object
}

header class OuterClassWithNamedCompanion {
    companion object Factory
}

header object OuterObject {
    object NestedObject
}

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt

impl class OuterClass {
    impl class NestedClass {
        impl class DeepNested {
            impl class Another {
                impl fun f(s: String) {}
                impl val p: Int = 42
            }
        }
    }

    impl inner class InnerClass {
        impl fun f(x: Int) {}
        impl val p: String = ""
    }

    impl companion object
}

impl class OuterClassWithNamedCompanion {
    impl companion object Factory
}

impl object OuterObject {
    impl object NestedObject
}
