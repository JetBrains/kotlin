// TARGET_BACKEND: JVM
// WITH_STDLIB

// MODULE: optimized
// !INHERIT_MULTIFILE_PARTS
// FILE: optimized.kt

@file:JvmMultifileClass
@file:JvmName("Optimized")
package optimized

fun test() {}

class RandomClass

// MODULE: main(optimized)
// FILE: default.kt

@file:JvmMultifileClass
@file:JvmName("Default")
package default

fun test() {}

// FILE: box.kt

fun isFlagSet(className: String): Boolean {
    val extraInt = Class.forName(className).getAnnotation(Metadata::class.java).extraInt
    return (extraInt and (1 shl 0)) != 0
}

fun box(): String {
    if (isFlagSet("default.Default"))
        return "Fail: inherited multifile parts flag should NOT be set by default for the facade"
    if (isFlagSet("default.Default__DefaultKt"))
        return "Fail: inherited multifile parts flag should NOT be set by default for the part"

    if (!isFlagSet("optimized.Optimized"))
        return "Fail: inherited multifile parts flag SHOULD be set by default for the facade"
    if (!isFlagSet("optimized.Optimized__OptimizedKt"))
        return "Fail: inherited multifile parts flag SHOULD be set by default for the part"

    if (isFlagSet("optimized.RandomClass"))
        return "Fail: inherited multifile parts flag should NOT be set by default for some random class if -Xmultifile-part-inherit is enabled"

    return "OK"
}
