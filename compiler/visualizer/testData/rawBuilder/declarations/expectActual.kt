expect class MyClass

expect fun foo(): String

//         Int
//         │
expect val x: Int

actual class MyClass

//               String
//               │
actual fun foo() = "Hello"

//         Int Int
//         │   │
actual val x = 42
