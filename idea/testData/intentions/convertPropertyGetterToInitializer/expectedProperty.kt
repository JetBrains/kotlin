// ENABLE_MULTIPLATFORM
// ERROR: Expected declaration must not have a body
// IS_APPLICABLE: false
expect class C {
    val p: Int
        <caret>get() = 1
}

actual class C {
    actual val p: Int = 1
}