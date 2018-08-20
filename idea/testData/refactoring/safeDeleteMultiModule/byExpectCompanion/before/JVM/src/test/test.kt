package test

actual class A {
    actual companion object {
        actual val useful: Int = 42
    }
}

val usage = A.useful