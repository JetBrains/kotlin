package test.pkg

actual class PointerEvent {
    actual val keyboardModifiers = PointerKeyboardModifiers(42)
}

internal actual typealias NativePointerKeyboardModifiers = Int

fun test() {
    val m = PointerEvent().keyboardModifiers.packedValue
}