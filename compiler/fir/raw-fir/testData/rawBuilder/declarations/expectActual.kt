expect class MyClass

expect fun foo(): String

expect val x: Int

actual class MyClass

actual fun foo() = "Hello"

actual val x = 42