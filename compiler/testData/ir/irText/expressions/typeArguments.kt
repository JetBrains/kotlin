// TARGET_BACKEND: JVM
// FIR_IDENTICAL
// WITH_STDLIB

fun test1(x: Any) =
        x is Array<*> && x.isArrayOf<String>()