// TARGET_BACKEND: JVM
// WITH_STDLIB

fun test1(x: Any) =
        x is Array<*> && x.isArrayOf<String>()