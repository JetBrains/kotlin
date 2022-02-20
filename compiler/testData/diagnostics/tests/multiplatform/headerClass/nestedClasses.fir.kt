// !DIAGNOSTICS: -UNUSED_PARAMETER
// MODULE: m1-common
// FILE: common.kt
import kotlin.reflect.KProperty

fun <T> lazy(initializer: () -> T): Lazy<T> = <!UNRESOLVED_REFERENCE!>TODO<!>()
interface Lazy<out T> {
    operator fun getValue(thisRef: Any?, property: KProperty<*>): T = <!UNRESOLVED_REFERENCE!>TODO<!>()
}

expect class OuterClass {
    class NestedClass {
        class DeepNested {
            class Another {
                fun f(s: String)
                val p: Int
                val c: Int = <!EXPECTED_PROPERTY_INITIALIZER!>1<!>
                val a: Int by <!EXPECTED_DELEGATED_PROPERTY!>lazy { 1 }<!>
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

// MODULE: m2-jvm()()(m1-common)
// FILE: jvm.kt

actual class OuterClass {
    actual class NestedClass {
        actual class DeepNested {
            actual class Another {
                actual fun f(s: String) {}
                actual val p: Int = 42
                actual val c: Int = 2
                actual val a: Int = 3
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
