// LANGUAGE: +MultiPlatformProjects
// MODULE: commonMain
// FILE: PointerEvent.kt

expect class PointerEvent {
    val keyboardModifiers: PointerKeyboardModifiers
}

expect class NativePointerKeyboardModifiers

@kotlin.jvm.JvmInline
value class PointerKeyboardModifiers(internal val packedValue: NativePointerKeyboardModifiers)

// MODULE: androidMain(commonMain)
// FILE: PointerEvent.android.kt

actual class PointerEvent {
    actual val <caret>keyboardModifiers = PointerKeyboardModifiers(42)
}

internal actual typealias NativePointerKeyboardModifiers = Int
