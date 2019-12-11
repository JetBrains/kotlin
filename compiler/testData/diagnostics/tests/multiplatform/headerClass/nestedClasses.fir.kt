// !LANGUAGE: +MultiPlatformProjects
// MODULE: m1-common
// FILE: common.kt

expect class OuterClass {
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

expect class OuterClassWithNamedCompanion {
    companion object Factory
}

expect object OuterObject {
    object NestedObject
}

// MODULE: m2-jvm(m1-common)
// FILE: jvm.kt

actual class OuterClass {
    actual class NestedClass {
        actual class DeepNested {
            actual class Another {
                actual fun f(s: String) {}
                actual val p: Int = 42
            }
        }
    }

    actual inner class InnerClass {
        actual fun f(x: Int) {}
        actual val p: String = ""
    }

    actual companion object
}

actual class OuterClassWithNamedCompanion {
    actual companion object Factory
}

actual object OuterObject {
    actual object NestedObject
}
