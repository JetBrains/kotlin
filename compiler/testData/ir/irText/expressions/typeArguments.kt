// TARGET_BACKEND: JVM
// FIR_IDENTICAL
// WITH_RUNTIME

fun test1(x: Any) =
        x is Array<*> && x.isArrayOf<String>()