package test.pkg

expect class PointerEvent {
    val keyboardModifiers: PointerKeyboardModifiers
}

expect class NativePointerKeyboardModifiers

@kotlin.jvm.JvmInline
value class PointerKeyboardModifiers(val packedValue: NativePointerKeyboardModifiers)