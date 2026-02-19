// NO_CHECK_LAMBDA_INLINING
// ^^^ JVM should be fixed by KT-74383

// This tests ensures that reference to an inline function is inlined.
// The actual check that no inline-able references are left out is carried out in
// validateIrAfterInliningAllFunctions and BytecodeInliningHandler.

// FILE: 1.kt
inline fun Int.toStr(): String = toChar().toString()

// FILE: 2.kt
fun box(): String {
    val o = 79::toStr
    val k = 75.toStr()
    val ok = o() + k
    return ok
}
